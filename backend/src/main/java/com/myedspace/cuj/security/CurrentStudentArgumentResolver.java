package com.myedspace.cuj.security;

import com.myedspace.cuj.domain.Student;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Injects the {@link Student} that {@link AuthInterceptor} resolved and stashed on the request. */
@Component
public class CurrentStudentArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentStudent.class)
                && Student.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        Object student = request.getAttribute(AuthInterceptor.STUDENT_ATTRIBUTE);
        if (student == null) {
            throw new IllegalStateException(
                    "No authenticated Student on the request. @CurrentStudent can only be used on "
                            + "endpoints covered by AuthInterceptor's path patterns (see WebConfig).");
        }
        return student;
    }
}
