/*
 * Copyright 2012-2020. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. More information from:
 *
 *        https://github.com/fenixsoft
 */

package com.github.fenixsoft.bookstore.domain.account.validation;

import com.github.fenixsoft.bookstore.domain.account.Account;
import com.github.fenixsoft.bookstore.domain.account.AccountRepository;
import com.github.fenixsoft.bookstore.domain.auth.AuthenticAccount;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * 用户对象校验器
 * <p>
 * 如，新增用户时，判断该用户对象是否允许唯一，在修改用户时，判断该用户是否存在
 *
 * @author icyfenix@gmail.com
 * @date 2020/3/11 14:22
 **/
public class AccountValidation<T extends Annotation> implements ConstraintValidator<T, Account> {

    @Inject
    protected AccountRepository repository;

    protected Predicate<Account> predicate = c -> true;

    @Override
    public boolean isValid(Account value, ConstraintValidatorContext context) {
        // 在 JPA 持久化时，默认采用 Hibernate 实现，插入、更新时都会调用 BeanValidationEventListener 进行验证
        // 而验证行为应该尽可能在外层进行，Resource 中已经通过 @Vaild 注解触发过一次验证，这里会导致重复执行
        //
        // 正常途径是使用分组验证避免，但 @Vaild 不支持分组，@Validated 支持，却又是 Spring 的私有标签
        // 另一个途径是设置 Hibernate 配置文件中的 javax.persistence.validation.mode 参数为“none”，这个参数在 Spring 的 yml 中未提供桥接
        // 为了避免涉及到数据库操作的验证重复进行，在这里做增加此空值判断，利用 Hibernate 验证时验证器不是被 Spring 创建的特点绕开
        return repository == null || predicate.test(value);
        // todo 确定一下这里的 repository == null 的结果？
        //  想要的效果应该是：
        //  JPA 持久化校验的时候为 true，就无需判断 Predicate 的结果，即省略了 JPA 持久化层次的再校验；
        //  @Valid 校验时为 false，则只需要看 Predicate 的结果；
        //  也就是说 JPA 持久化校验的 Hibernate 不是由 Spring 创建的？
        //  Debug 方法：
        //  1. 打断点，在 @Valid 和操作 Repository 之前查看一下这里的结果；
        //  2. 注入一个 CrudRepository 看看数量
    }

    public static class ExistsAccountValidator extends AccountValidation<ExistsAccount> {
        public void initialize(ExistsAccount constraintAnnotation) {
            predicate = c -> repository.existsById(c.getId());
        }
    }

    public static class AuthenticatedAccountValidator extends AccountValidation<AuthenticatedAccount> {
        public void initialize(AuthenticatedAccount constraintAnnotation) {
            predicate = c -> {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if ("anonymousUser".equals(principal)) {
                    return false;
                } else {
                    AuthenticAccount loginUser = (AuthenticAccount) principal;
                    return c.getId().equals(loginUser.getId());
                }
            };
        }
    }

    public static class UniqueAccountValidator extends AccountValidation<UniqueAccount> {
        public void initialize(UniqueAccount constraintAnnotation) {
            predicate = c -> !repository.existsByUsernameOrEmailOrTelephone(c.getUsername(), c.getEmail(), c.getTelephone());
        }
    }

    public static class NotConflictAccountValidator extends AccountValidation<NotConflictAccount> {
        public void initialize(NotConflictAccount constraintAnnotation) {
            predicate = c -> {
                Collection<Account> collection = repository.findByUsernameOrEmailOrTelephone(c.getUsername(), c.getEmail(), c.getTelephone());
                // 将用户名、邮件、电话改成与现有完全不重复的，或者只与自己重复的，就不算冲突
                return collection.isEmpty() || (collection.size() == 1 && collection.iterator().next().getId().equals(c.getId()));
            };
        }
    }

}
