<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cda="urn:hl7-org:v3"
    exclude-result-prefixes="cda">

    <xsl:output method="html" indent="yes" encoding="UTF-8"/>

    <xsl:variable name="title">
        <xsl:choose>
            <xsl:when test="string-length(//cda:ClinicalDocument/cda:title) &gt; 0">
                <xsl:value-of select="//cda:ClinicalDocument/cda:title"/>
            </xsl:when>
            <xsl:when test="//cda:ClinicalDocument/cda:code/@displayName">
                <xsl:value-of select="//cda:ClinicalDocument/cda:code/@displayName"/>
            </xsl:when>
            <xsl:otherwise>Documento Clínico</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="/">
        <html>
            <head>
                <title><xsl:value-of select="$title"/></title>
                <style type="text/css">
                    body { font-family: Arial, sans-serif; font-size: 14px; color: #222; }
                    h1, h2, h3 { color: #0d47a1; }
                    table { width: 100%; border-collapse: collapse; margin: 8px 0; }
                    th, td { border: 1px solid #ccc; padding: 6px; vertical-align: top; }
                    .meta td { background: #f6f9ff; }
                </style>
            </head>
            <body>
                <h1><xsl:value-of select="$title"/></h1>
                <xsl:call-template name="document-meta"/>
                <xsl:choose>
                    <xsl:when test="//cda:ClinicalDocument/cda:component/cda:structuredBody">
                        <xsl:apply-templates select="//cda:ClinicalDocument/cda:component/cda:structuredBody"/>
                    </xsl:when>
                    <xsl:when test="//cda:ClinicalDocument/cda:component/cda:nonXMLBody">
                        <xsl:apply-templates select="//cda:ClinicalDocument/cda:component/cda:nonXMLBody"/>
                    </xsl:when>
                </xsl:choose>
            </body>
        </html>
    </xsl:template>

    <xsl:template name="document-meta">
        <table class="meta">
            <tr>
                <td width="25%"><b>Documento</b></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="//cda:ClinicalDocument/cda:id/@extension">
                            <xsl:value-of select="//cda:ClinicalDocument/cda:id/@extension"/>
                            <xsl:text> (</xsl:text>
                            <xsl:value-of select="//cda:ClinicalDocument/cda:id/@root"/>
                            <xsl:text>)</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="//cda:ClinicalDocument/cda:id/@root"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <tr>
                <td><b>Fecha generación</b></td>
                <td><xsl:value-of select="//cda:ClinicalDocument/cda:effectiveTime/@value"/></td>
            </tr>
            <tr>
                <td><b>Paciente</b></td>
                <td>
                    <xsl:apply-templates select="//cda:ClinicalDocument/cda:recordTarget/cda:patientRole" mode="brief"/>
                </td>
            </tr>
        </table>
    </xsl:template>

    <xsl:template match="cda:patientRole" mode="brief">
        <xsl:choose>
            <xsl:when test="cda:patient/cda:name">
                <xsl:for-each select="cda:patient/cda:name">
                    <xsl:value-of select="normalize-space(string(.))"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>Sin nombre</xsl:otherwise>
        </xsl:choose>
        <xsl:text> | Documento: </xsl:text>
        <xsl:value-of select="cda:id/@extension"/>
    </xsl:template>

    <xsl:template match="cda:structuredBody">
        <xsl:for-each select="cda:component/cda:section">
            <h2><xsl:value-of select="cda:title"/></h2>
            <div>
                <xsl:apply-templates select="cda:text | cda:entry"/>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="cda:text">
        <div><xsl:copy-of select="node()"/></div>
    </xsl:template>

    <xsl:template match="cda:entry">
        <xsl:if test="cda:observation or cda:act or cda:procedure or cda:substanceAdministration">
            <table>
                <tr>
                    <th>Tipo</th>
                    <th>Detalle</th>
                </tr>
                <xsl:apply-templates select="cda:observation | cda:act | cda:procedure | cda:substanceAdministration"/>
            </table>
        </xsl:if>
    </xsl:template>

    <xsl:template match="cda:observation | cda:act | cda:procedure | cda:substanceAdministration">
        <tr>
            <td><xsl:value-of select="local-name()"/></td>
            <td>
                <xsl:if test="@classCode"><xsl:text>class=</xsl:text><xsl:value-of select="@classCode"/><xsl:text> </xsl:text></xsl:if>
                <xsl:if test="@moodCode"><xsl:text>mood=</xsl:text><xsl:value-of select="@moodCode"/><xsl:text> </xsl:text></xsl:if>
                <xsl:if test="cda:code/@displayName">
                    <xsl:value-of select="cda:code/@displayName"/>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="cda:nonXMLBody">
        <xsl:choose>
            <xsl:when test="cda:text/@mediaType='text/html'">
                <div>Contenido HTML embebido.</div>
            </xsl:when>
            <xsl:when test="cda:text/@mediaType='application/pdf'">
                <div>Documento PDF embebido.</div>
            </xsl:when>
            <xsl:otherwise>
                <pre><xsl:value-of select="normalize-space(string(cda:text))"/></pre>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
