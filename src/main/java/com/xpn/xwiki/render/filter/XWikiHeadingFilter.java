/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.render.filter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.util.TOCGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.filter.CacheFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.regex.MatchResult;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A customized version of Radeox Heading Filter
 */
public class XWikiHeadingFilter extends LocaleRegexTokenFilter implements CacheFilter
{
    private static Log log = LogFactory.getLog(XWikiHeadingFilter.class);

    private final String TOC_NUMBERED = "tocNumbered";

    private final String TOC_DATA = "tocData";

    private MessageFormat formatter;

    private int sectionNumber = 0;

    protected String getLocaleKey()
    {
        return "filter.heading";
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        buffer.append(handleMatch(result, context));
    }

    public void setInitialContext(InitialRenderContext context)
    {
        super.setInitialContext(context);
        String outputTemplate = outputMessages.getString(getLocaleKey() + ".print");
        formatter = new MessageFormat("");
        formatter.applyPattern(outputTemplate);
    }

    public String handleMatch(MatchResult result, FilterContext context)
    {
        String id = null;
        String title = result.group(0);
        String level = result.group(1);
        int level_i = (level.length() + 3) / 2;
        String hlevel = (level_i <= 6 ? level_i : 6) + "";
        String text = result.group(3);
        String numbering = "";

        RenderContext rcontext = context.getRenderContext();
        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) rcontext.getRenderEngine()).getContext();
        XWikiDocument doc = xcontext.getDoc();

        log.debug("Processing '" + text + "'");
        // generate unique ID of the heading
        List processedHeadings = (List) rcontext.get("processedHeadings");
        if (processedHeadings == null) {
            processedHeadings = new ArrayList();
            rcontext.set("processedHeadings", processedHeadings);
        }
        id = TOCGenerator.makeHeadingID(text, 0, xcontext);
        int occurence = 0;
        while (processedHeadings.contains(id)) {
            occurence++;
            id = TOCGenerator.makeHeadingID(text, occurence, xcontext);
        }
        processedHeadings.add(id);
        log.debug("Generated heading id '" + id + "'");

        //  add numbering if the flag is set
        if (xcontext.containsKey(TOC_NUMBERED) &&
            ((Boolean) xcontext.get(TOC_NUMBERED)).booleanValue())
        {
            if (xcontext.containsKey(TOC_DATA)) {
                Map tocEntry = (Map) ((Map) xcontext.get(TOC_DATA)).get(id);
                if (tocEntry != null) {
                    numbering = (String) tocEntry.get(TOCGenerator.TOC_DATA_NUMBERING) + " ";
                }
            }
        }

        String heading = formatter.format(
            new Object[] {id, level.replaceAll("\\.", "-"), numbering, text, hlevel});

        // Only show the section edit button for view action and when the user has edit rights on
        // the current document
        boolean showEditButton = false;
        if (xcontext.getWiki().hasSectionEdit(xcontext) && ("view".equals(xcontext.getAction()))) {
            try {
                // TODO: The user should always be set and never be null when this code gets
                // executed. Unfortunately this is currently happening. It should be set to XWiki
                // Guest immediatly in the initialization phase.
                // TODO: Similarly the current document should never be null when this code gets
                // executed as it would mean we're trying to render the headings for a null
                // document and that doesn't make sense... 
                if ((doc != null) && ((xcontext.getUser() != null)
                    && xcontext.getWiki().getRightService().hasAccessLevel(
                    "edit", xcontext.getUser(), doc.getFullName(), xcontext)))
                {
                    showEditButton = true;
                }
            } catch (XWikiException e) {
                // TODO: Remove this try/catch block by removing the throw exception on
                // hasAccessLevel() as it never throws any exception...
            }
        }

        Object beforeAction = xcontext.get("action");
        if (showEditButton) {
            if (beforeAction != null) {
                if (!beforeAction.toString().equals("HeadingFilter")) {
                    xcontext.put("action", "HeadingFilter");
                    sectionNumber = 0;
                }
            }

            if (level.equals("1") || level.equals("1.1")) {
                if (doc != null && doc.getContent().indexOf(title.trim()) != -1) {
                    sectionNumber++;
                    StringBuffer editparams = new StringBuffer();
                    if (xcontext.getWiki().getEditorPreference(xcontext).equals("wysiwyg")) {
                        editparams.append("xpage=wysiwyg&amp;section=").append(sectionNumber);
                    } else {
                        editparams.append("section=").append(sectionNumber);
                    }
                    try {
                        if ((xcontext.getWiki().isMultiLingual(xcontext)) &&
                            (doc.getRealLanguage(xcontext) != null))
                        {
                            editparams.append("&amp;language=")
                                .append(doc.getRealLanguage(xcontext));
                        }
                    } catch (XWikiException e) {
                    }

                    String url = doc.getURL("edit", editparams.toString(), xcontext);
                    return heading + "<span class='edit_section'>&#91;"
                        + "<a style='text-decoration: none;' title='Edit section: "
                        + text.replaceAll("'", "&#39;") + "' href='" + url + "'>" + "edit"
                        + "</a>&#93;</span>";
                }
            }
        }

        return heading;
    }
}
