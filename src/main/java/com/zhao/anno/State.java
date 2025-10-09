package com.zhao.anno;

import com.zhao.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Documented//元注解
//元注解，表示这个注解可以用在哪些，这个注解用在属性上，只保留FIELD即可
@Target({ElementType.FIELD})
//元注解，说明这个注解保留到那个阶段，RUNTIME保留到运行阶段，即@Stade保留到运行阶段
@Retention(RetentionPolicy.RUNTIME)
//指定谁给这个注解提供校验的规则
@Constraint( validatedBy = { StateValidation.class } )//指定提供校验规则的类
//不需要
//@Repeatable(List.class)
public @interface State {

    //提供校验失败后的提示信息
    String message() default "state参数的值只能是已发布或者草稿！";

    //指定分组
    Class<?>[] groups() default {};

    //负载  可以获取到@Stade的附加信息 一般用不上但必须提供
    Class<? extends Payload>[] payload() default {};
}
