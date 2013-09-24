/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wikistream.xar.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.wikistream.filter.WikiClassFilter;
import org.xwiki.wikistream.xar.internal.XARUtils.Parameter;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class XARClassModel
{
    public static final String ELEMENT_CLASS = "class";

    public static final String ELEMENT_NAME = "name";

    public static final String ELEMENT_CUSTOMCLASS = "customClass";

    public static final String ELEMENT_CUSTOMMAPPING = "customMapping";

    public static final String ELEMENT_SHEET_DEFAULTVIEW = "defaultViewSheet";

    public static final String ELEMENT_SHEET_DEFAULTEDIT = "defaultEditSheet";

    public static final String ELEMENT_DEFAULTSPACE = "defaultWeb";

    public static final String ELEMENT_NAMEFIELD = "nameField";

    public static final String ELEMENT_VALIDATIONSCRIPT = "validationScript";

    // Utils

    public static final Map<String, Parameter> CLASS_PARAMETERS = new HashMap<String, Parameter>()
    {
        {
            put(ELEMENT_CUSTOMCLASS, new Parameter(WikiClassFilter.PARAMETER_CUSTOMCLASS));
            put(ELEMENT_CUSTOMMAPPING, new Parameter(WikiClassFilter.PARAMETER_CUSTOMMAPPING));
            put(ELEMENT_SHEET_DEFAULTVIEW, new Parameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW));
            put(ELEMENT_SHEET_DEFAULTEDIT, new Parameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT));
            put(ELEMENT_DEFAULTSPACE, new Parameter(WikiClassFilter.PARAMETER_DEFAULTSPACE));
            put(ELEMENT_NAMEFIELD, new Parameter(WikiClassFilter.PARAMETER_NAMEFIELD));
            put(ELEMENT_VALIDATIONSCRIPT, new Parameter(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT));
        }
    };
}
