/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.gmbal.impl ;

// import com.sun.jmx.mbeanserver.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.management.Descriptor;
import javax.management.modelmbean.DescriptorSupport;

public class DescriptorUtility {
    private DescriptorUtility() {}

    public static final Descriptor EMPTY_DESCRIPTOR =
        makeDescriptor( new HashMap<String,Object>() );

    public static Descriptor makeDescriptor( Map<String, ?> fields ) {
        if (fields == null) {
            throw Exceptions.self.nullMap() ;
        }
        SortedMap<String, Object> map =
            new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.equals("")) {
                throw Exceptions.self.badFieldName() ;
            }
            if (map.containsKey(name)) {
                throw Exceptions.self.duplicateFieldName( name ) ;
            }
            map.put(name, entry.getValue());
        }
        int size = map.size();
        String[] names = map.keySet().toArray(new String[size]);
        Object[] values = map.values().toArray(new Object[size]);
        return new DescriptorSupport( names, values ) ;
    }

    private static SortedMap<String, ?> makeMap(String[] fieldNames,
                                                Object[] fieldValues) {
        if (fieldNames == null || fieldValues == null) {
            throw Exceptions.self.nullArrayParameter() ;
        }
        if (fieldNames.length != fieldValues.length) {
            throw Exceptions.self.differentSizeArrays() ;
        }
        SortedMap<String, Object> map =
                new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < fieldNames.length; i++) {
            String name = fieldNames[i];
            if (name == null || name.equals("")) {
                throw Exceptions.self.badFieldName() ;
            }
            Object old = map.put(name, fieldValues[i]);
            if (old != null) {
                throw Exceptions.self.duplicateFieldName( name ) ;
            }
        }
        return map;
    }

    private static SortedMap<String, ?> makeMap(String[] fields) {
        if (fields == null) {
            throw Exceptions.self.nullFieldsParameter() ;
        }
        String[] fieldNames = new String[fields.length];
        String[] fieldValues = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            int eq = field.indexOf('=');
            if (eq < 0) {
                throw Exceptions.self.badFieldFormat( field ) ;
            }
            fieldNames[i] = field.substring(0, eq);
            // makeMap will catch the case where the name is empty
            fieldValues[i] = field.substring(eq + 1);
        }
        return makeMap(fieldNames, fieldValues);
    }

    public static Descriptor union(Descriptor... descriptors) {
        Map<String, Object> map =
            new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        Descriptor biggestImmutable = EMPTY_DESCRIPTOR;
        for (Descriptor d : descriptors) {
            if (d != null) {
                String[] names = d.getFieldNames();
                for (String n : names) {
                    Object v = d.getFieldValue(n);
                    Object old = map.put(n, v);
                    if (old != null) {
                        boolean equal;
                        if (old.getClass().isArray()) {
                            equal = Arrays.deepEquals(new Object[] {old},
                                                      new Object[] {v});
                        } else {
                            equal = old.equals(v);
                        }
                        if (!equal) {
                            throw Exceptions.self.excForUnion( n, old, v ) ;
                        }
                    }
                }
            }
        }

        return makeDescriptor(map);
    }

    public static Map<String,?> getMap( Descriptor desc ) {
        String[] names = desc.getFieldNames() ;
        Object[] values = desc.getFieldValues(names);
	return makeMap(names, values);
    }
}