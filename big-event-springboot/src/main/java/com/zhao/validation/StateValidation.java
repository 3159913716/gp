package com.zhao.validation;

import com.zhao.anno.State;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

//ConstraintValidator<Stade, String> 说明给@Stade提供校验规则，第二个泛型说明校验的数据类型
public class StateValidation implements ConstraintValidator<State, String> {
    /**
     * @param s 将来要校验的数据
     * @param constraintValidatorContext 你好
     *
     * @return 如果返回false则校验不通过，否者反之
     * */
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        //提供校验规则
        if (s == null) {
            return false;
        }
//        if (s.equals("已发布") || s.equals("草稿")) {
//            return true;
//        }
        return s.equals("已发布") || s.equals("草稿");
    }
}
