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
package org.xwiki.rendering.internal.configuration;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.CoreConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration}.
 * 
 * @version $Id$
 * @since 8.2M1
 */
@ComponentTest
class DefaultExtendedRenderingConfigurationTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private DefaultExtendedRenderingConfiguration defaultExtendedRenderingConfiguration;

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @BeforeComponent
    void setup() throws Exception
    {
        Provider<ComponentManager> contextComponentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    void getImageWidthLimit() throws Exception
    {
        ConfigurationSource source = componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageWidthLimit", -1)).thenReturn(100);

        assertEquals(100, this.defaultExtendedRenderingConfiguration.getImageWidthLimit());
    }

    @Test
    void getImageHeightLimit() throws Exception
    {
        ConfigurationSource source = componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageHeightLimit", -1)).thenReturn(150);

        assertEquals(150, this.defaultExtendedRenderingConfiguration.getImageHeightLimit());
    }

    @Test
    void isImageDimensionsIncludedInImageURL() throws Exception
    {
        ConfigurationSource source = this.componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageDimensionsIncludedInImageURL", true)).thenReturn(false);
        assertFalse(this.defaultExtendedRenderingConfiguration.isImageDimensionsIncludedInImageURL());

        when(source.getProperty("rendering.imageDimensionsIncludedInImageURL", true)).thenReturn(true);
        assertTrue(this.defaultExtendedRenderingConfiguration.isImageDimensionsIncludedInImageURL());
    }

    @Test
    void getConfiguredAndDisabledSyntaxes() throws Exception
    {
        ConfigurationSource renderingSource = this.componentManager.getInstance(ConfigurationSource.class, "rendering");
        when(renderingSource.getProperty("disabledSyntaxes")).thenReturn(Arrays.asList("syntax1/1.0", "syntax2/1.0"));

        CoreConfiguration coreConfiguration = componentManager.getInstance(CoreConfiguration.class);
        Syntax defaultSyntax = new Syntax(new SyntaxType("xwiki", "XWiki"), "2.1");
        when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(defaultSyntax);

        // Register some Syntaxes for the test

        Parser defaultSyntaxParser = componentManager.registerMockComponent(Parser.class, "xwiki/2.1");
        when(defaultSyntaxParser.getSyntax()).thenReturn(defaultSyntax);

        Parser syntax1Parser = componentManager.registerMockComponent(Parser.class, "syntax1/1.0");
        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);
        Parser syntax2Parser = componentManager.registerMockComponent(Parser.class, "syntax2/1.0");
        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        when(this.syntaxRegistry.resolveSyntax("syntax1/1.0")).thenReturn(syntax1);
        when(this.syntaxRegistry.resolveSyntax("syntax2/1.0")).thenReturn(syntax2);

        List<Syntax> disabledSyntaxes = this.defaultExtendedRenderingConfiguration.getDisabledSyntaxes();
        assertEquals(2, disabledSyntaxes.size());
        assertTrue(disabledSyntaxes.contains(syntax1));
        assertTrue(disabledSyntaxes.contains(syntax2));

        List<Syntax> configuredSyntaxes = this.defaultExtendedRenderingConfiguration.getConfiguredSyntaxes();
        assertEquals(1, configuredSyntaxes.size());
        assertTrue(configuredSyntaxes.contains(defaultSyntax));
    }

    @Test
    void getDefaultContentSyntax() throws Exception
    {
        CoreConfiguration coreConfiguration = componentManager.getInstance(CoreConfiguration.class);
        when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_2_1);
        assertEquals(Syntax.XWIKI_2_1, this.defaultExtendedRenderingConfiguration.getDefaultContentSyntax());
    }
}
