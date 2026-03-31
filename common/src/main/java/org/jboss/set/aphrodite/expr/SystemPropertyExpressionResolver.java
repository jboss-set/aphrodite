package org.jboss.set.aphrodite.expr;

public class SystemPropertyExpressionResolver extends AbstractPropertyExpressionResolver {

    @Override
    public String resolve(String expression) {
        return resolve(System::getProperty, expression);
    }

}
