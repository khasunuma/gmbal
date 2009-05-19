/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * file and include the License file at legal/LICENSE.TXT.
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
 * 
 */ 
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.gmbal.impl;

import java.util.LinkedHashSet;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;

/** A simple class that implements deferred registration.
 * When registration is suspended, mbean registrations are
 * queued until registration is resumed, at which time the
 * registration are processed in order.
 *
 * @author ken
 */
public class JMXRegistrationManager {
    private boolean isSuspended = false ;
    private LinkedHashSet<MBeanImpl> deferredRegistrations =
        new LinkedHashSet<MBeanImpl>() ;

    public synchronized void suspendRegistration() {
        isSuspended = true ;
    }

    public synchronized void resumeRegistration() {
        isSuspended = false ;
        for (MBeanImpl mb : deferredRegistrations) {
            try {
                mb.register();
            } catch (JMException ex) {
                Exceptions.self.deferredRegistrationException( ex, mb ) ;
            }
        }

        deferredRegistrations.clear() ;
    }

    public synchronized void register( MBeanImpl mb )
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException {

        if (isSuspended) {
            deferredRegistrations.add( mb ) ;
        } else {
            mb.register() ;
        }
    }

    public synchronized void unregister( MBeanImpl mb )
        throws InstanceNotFoundException, MBeanRegistrationException {

        if (isSuspended) {
            deferredRegistrations.remove(mb) ;
        }

        // Always unregister
        mb.unregister() ;
    }
}