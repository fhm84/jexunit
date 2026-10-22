package com.jexunit.core.data;

import ognl.AbstractMemberAccess;
import ognl.OgnlContext;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * OGNL member access allowing public members only. The column headers of the excel files are OGNL expressions, so they
 * must not reach private, protected or package-private members. Public members of non-public classes are accessible,
 * as OGNL handles them itself.
 *
 * @author fabian
 */
public class PublicMemberAccess extends AbstractMemberAccess {

    @Override
    public boolean isAccessible(final OgnlContext context, final Object target, final Member member,
                                final String propertyName) {
        return Modifier.isPublic(member.getModifiers());
    }

}
