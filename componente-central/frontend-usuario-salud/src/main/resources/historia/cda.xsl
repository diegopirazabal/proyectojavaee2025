<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" />

  <xsl:template match="/">
    <html>
      <head>
        <meta charset="UTF-8"/>
        <title>
          <xsl:choose>
            <xsl:when test="/documento/title"><xsl:value-of select="/documento/title"/></xsl:when>
            <xsl:otherwise>Documento Clínico</xsl:otherwise>
          </xsl:choose>
        </title>
        <style>
          body { font-family: Arial, sans-serif; margin: 1.5rem; }
          h1 { font-size: 1.4rem; margin-bottom: .25rem; }
          .meta { color:#555; margin-bottom: 1rem; }
          .section { margin-bottom: 1rem; }
          table { border-collapse: collapse; width: 100%; }
          th, td { border: 1px solid #ddd; padding: .4rem; text-align: left; }
          th { background: #f5f5f5; }
        </style>
      </head>
      <body>
        <h1>
          <xsl:choose>
            <xsl:when test="/documento/title"><xsl:value-of select="/documento/title"/></xsl:when>
            <xsl:otherwise>Documento Clínico</xsl:otherwise>
          </xsl:choose>
        </h1>
        <div class="meta">
          Paciente: <xsl:value-of select="/documento/paciente"/>
          <xsl:text> · Fecha: </xsl:text>
          <xsl:value-of select="/documento/fecha"/>
        </div>

        <div class="section">
          <h2>Resumen</h2>
          <div><xsl:value-of select="/documento/resumen"/></div>
        </div>

        <xsl:if test="/documento/observaciones/item">
          <div class="section">
            <h2>Observaciones</h2>
            <table>
              <tr><th>Tipo</th><th>Descripción</th></tr>
              <xsl:for-each select="/documento/observaciones/item">
                <tr>
                  <td><xsl:value-of select="@tipo"/></td>
                  <td><xsl:value-of select="."/></td>
                </tr>
              </xsl:for-each>
            </table>
          </div>
        </xsl:if>
      </body>
    </html>
  </xsl:template>
  
</xsl:stylesheet>

