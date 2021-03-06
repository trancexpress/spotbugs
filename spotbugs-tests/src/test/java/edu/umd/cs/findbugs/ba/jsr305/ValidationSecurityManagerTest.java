/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba.jsr305;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

import org.junit.Test;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * @author pugh
 */
public class ValidationSecurityManagerTest {


    private static final SlashedClassName ANNOTATION = AnnotationTemplate.class.getAnnotation(SlashedClassName.class);

    static class BadValidator implements TypeQualifierValidator<SlashedClassName> {

        @Override
        public @Nonnull When forConstantValue(@Nonnull SlashedClassName annotation, Object value) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    System.out.println("bang");
                }
            };
            t.start();
            return When.NEVER;
        }

    }

    public When test(TypeQualifierValidator<SlashedClassName> validator) {
        return ValidationSecurityManager.sandboxedValidation(ANNOTATION, validator, "java/lang/String");

    }

    public void havePermissions() throws InterruptedException {
        final AtomicBoolean b = new AtomicBoolean(false);
        Thread t = new Thread() {
            @Override
            public void run() {
                b.set(true);
            }
        };
        t.start();
        t.join();
        assertEquals(true, b.get());
        for (File f : File.listRoots()) {
            f.listFiles();
        }
    }

    @SlashedClassName
    static class AnnotationTemplate {
    }

    @Test
    public void test() throws InterruptedException {
        ValidationSecurityManager sm = ValidationSecurityManager.INSTANCE;
        SecurityManager old = System.getSecurityManager();
        System.setSecurityManager(sm);
        TypeQualifierValidator<SlashedClassName> goodValidator = new SlashedClassName.Checker();
        havePermissions();
        assertEquals(When.ALWAYS, test(goodValidator));
        try {
            test(new BadValidator());
            fail("Should have thrown SecurityException");
        } catch (SecurityException e) {
            assert true;
        }
        havePermissions();
        System.setSecurityManager(old);
        havePermissions();


    }

}
