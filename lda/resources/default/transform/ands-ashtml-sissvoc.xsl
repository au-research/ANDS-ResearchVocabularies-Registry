<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">

  <!-- This is the ANDS custom XSL transform to produce the HTML
       pages for SISSVoc. It is implemented as a set of overrides
       of the original SISSVoc stylesheet. -->
  
  <!-- First, import the original SISSVoc stylesheet -->
  <xsl:import href="ashtml-sissvoc.xsl" />

  <!-- Now, apply the ANDS customizations as patches. -->

  <!-- Patch 1 -->
  <!-- These are not overrides, but additional variables that can
       be set in a spec file. They are used by Patch 3. -->
  <xsl:param name="_ANDS_vocabName">Unnamed Vocabulary</xsl:param>
  <xsl:param name="_ANDS_vocabMore" />
  <xsl:param name="_ANDS_vocabAPIDoco" />

  <!-- Patch 2 -->
  <!-- Adjust the path to favicon.ico. -->
  <xsl:template match="result" mode="meta">
    <link rel="shortcut icon" href="/favicon.ico" type="image/x-icon" />
    <xsl:apply-templates select="first | prev | next | last"
                         mode="metalink" />
    <xsl:apply-templates select="hasFormat/item" mode="metalink" />
  </xsl:template>

  <!-- Patch 3 -->
  <!-- Discard the original setting of the h1 header, and use the
       additional variables supported by Patch 1, if provided. -->
  <xsl:template match="result" mode="header">
    <nav class="site">
      <xsl:apply-templates select="." mode="formats" />
    </nav>
    <header>
      <h1><xsl:value-of select="$_ANDS_vocabName"/></h1>
      <xsl:if test="$_ANDS_vocabMore != ''"
              ><p><a href="{$_ANDS_vocabMore}"
                     target="_blank"><i>(more information)</i></a></p></xsl:if>
      <xsl:if test="$_ANDS_vocabAPIDoco != ''"
              ><p><a href="{$_ANDS_vocabAPIDoco}"
                     target="_blank"><i>(web service API)</i></a></p></xsl:if>
    </header>
  </xsl:template>

  <!-- Patch 4 -->
  <!-- Show labels for nested resources, as implemented
       by Leo: https://github.com/epimorphics/elda/issues/143
  -->
  <xsl:template match="*[@href]" mode="content">
    <xsl:param name="nested" select="false()" />
    <xsl:choose>
      <xsl:when test="$nested">
	<xsl:apply-templates select="." mode="table" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="/result/items/item[@href = current()/@href]">
            <xsl:apply-templates
                select="/result/items/item[@href = current()/@href]"
                mode="link">
              <xsl:with-param name="content">
                <xsl:apply-templates
                    select="/result/items/item[@href = current()/@href]"
                    mode="name"/>
              </xsl:with-param>
            </xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
	    <xsl:apply-templates select="." mode="link">
	      <xsl:with-param name="content">
	        <xsl:call-template name="lastURIpart">
	          <xsl:with-param name="uri" select="@href" />
	        </xsl:call-template>
	      </xsl:with-param>
	    </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Sigh. Because import precedence takes "priority" over numbered
       priorities, we must also import all of the other templates with
       mode="content", leaving them unchanged.
       Therefore, when the original ashtml-sissvoc.xsl is updated,
       must check to see if any of these templates has been touched;
       if so, copy the updated versions here.
  -->

  <xsl:template match="result" mode="content" priority="10">
	<xsl:apply-templates select="." mode="topnav" />
	<div id="result">
		<div class="panel">
			<xsl:choose>
				<xsl:when test="items">
					<header><h1>Search Results</h1></header>
					<xsl:apply-templates select="items" mode="content" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates select="primaryTopic" mode="content" />
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</div>
	<xsl:apply-templates select="." mode="bottomnav" />
</xsl:template>

<xsl:template match="/result/primaryTopic" mode="content" priority="10">
	<header>
		<h1><xsl:apply-templates select="." mode="name" /></h1>
		<p class="id"><a href="{@href}"><xsl:value-of select="@href" /></a></p>
	</header>
	<section>
		<xsl:apply-templates select="." mode="header" />
		<xsl:apply-templates select="." mode="table" />
		<xsl:apply-templates select="." mode="footer" />
	</section>
</xsl:template>

<xsl:template match="items" mode="content" priority="10">
	<xsl:choose>
		<xsl:when test="item[@href]">
			<xsl:apply-templates mode="section" />
		</xsl:when>
		<xsl:otherwise>
			<section>
				<p>No results</p>
			</section>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="items/item" mode="content" priority="20">
	<xsl:apply-templates select="." mode="table" />
</xsl:template>

<xsl:template match="*[item]" mode="content" priority="4">
	<xsl:param name="nested" select="false()" />
	<xsl:variable name="label" select="key('propertyTerms', $label-uri)/label" />
	<xsl:variable name="prefLabel" select="key('propertyTerms', $prefLabel-uri)/label" />
	<xsl:variable name="altLabel" select="key('propertyTerms', $altLabel-uri)/label" />
	<xsl:variable name="name" select="key('propertyTerms', $name-uri)/label" />
	<xsl:variable name="title" select="key('propertyTerms', $title-uri)/label" />
	<xsl:variable name="isLabelParam">
		<xsl:apply-templates select="." mode="isLabelParam" />
	</xsl:variable>
	<xsl:variable name="anyItemHasNonLabelProperties">
		<xsl:apply-templates select="." mode="anyItemHasNonLabelProperties" />
	</xsl:variable>
	<xsl:variable name="anyItemIsHighestDescription">
		<xsl:apply-templates select="." mode="anyItemIsHighestDescription" />
	</xsl:variable>
	<xsl:choose>
		<xsl:when test="$anyItemHasNonLabelProperties = 'true' and $anyItemIsHighestDescription = 'true'">
			<xsl:for-each select="item">
				<xsl:sort select="*[name(.) = $prefLabel]" />
				<xsl:sort select="*[name(.) = $name]" />
				<xsl:sort select="*[name(.) = $title]" />
				<xsl:sort select="*[name(.) = $label]" />
				<xsl:sort select="*[name(.) = $altLabel]" />
				<xsl:sort select="@href" />
				<xsl:apply-templates select="." mode="content">
					<xsl:with-param name="nested" select="$nested" />
				</xsl:apply-templates>
			</xsl:for-each>
		</xsl:when>
		<xsl:otherwise>
			<table>
				<xsl:for-each select="item">
					<xsl:sort select="*[name(.) = $prefLabel]" />
					<xsl:sort select="*[name(.) = $name]" />
					<xsl:sort select="*[name(.) = $title]" />
					<xsl:sort select="*[name(.) = $label]" />
					<xsl:sort select="*[name(.) = $altLabel]" />
					<xsl:sort select="@href" />
					<xsl:apply-templates select="." mode="row" />
				</xsl:for-each>
			</table>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="*[*]" mode="content" priority="3">
	<xsl:param name="nested" select="false()" />
	<xsl:variable name="hasNonLabelProperties">
		<xsl:apply-templates select="." mode="hasNonLabelProperties" />
	</xsl:variable>
	<xsl:variable name="isHighestDescription">
		<xsl:apply-templates select="." mode="isHighestDescription" />
	</xsl:variable>
	<xsl:choose>
		<xsl:when test="$nested or ($hasNonLabelProperties = 'true' and $isHighestDescription = 'true')">
			<xsl:apply-templates select="." mode="table" />
		</xsl:when>
		<xsl:otherwise>
			<xsl:apply-templates select="." mode="link">
				<xsl:with-param name="content">
					<xsl:apply-templates select="." mode="name" />
				</xsl:with-param>
			</xsl:apply-templates>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="*" mode="content">
	<xsl:value-of select="." />
</xsl:template>

  <!-- End of verbatim-copied templates. -->


  <!-- Patch 5 -->
  <!-- Remove "magnifying glass" search links by removing the _first_
       element like this:
       <a rel="nofollow" title="more like this">...
       (The original template has also been reformatted here.)

       See CC-2089 for a proposed change (to do with specifying the
       vocabulary's languages in its spec file), which, if
       implemented, would allow restoration of these search links.
  -->
  <xsl:template match="*" mode="filter">
    <xsl:param name="paramName">
      <xsl:apply-templates select="." mode="paramName" />
    </xsl:param>
    <xsl:param name="value" select="." />
    <xsl:param name="label">
      <xsl:apply-templates select="." mode="value" />
    </xsl:param>
    <xsl:param name="datatype" select="@datatype" />
    <xsl:param name="hasNonLabelProperties">
      <xsl:apply-templates select="." mode="hasNonLabelProperties" />
    </xsl:param>
    <xsl:param name="hasNoLabelProperties">
      <xsl:apply-templates select="." mode="hasNoLabelProperties" />
    </xsl:param>
    <xsl:variable name="paramValue">
      <xsl:call-template name="paramValue">
	<xsl:with-param name="uri">
	  <xsl:apply-templates select="/result" mode="searchURI" />
	</xsl:with-param>
	<xsl:with-param name="param" select="$paramName" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$value = ''" />
      <xsl:when test="$hasNonLabelProperties = 'true' and
                      $hasNoLabelProperties = 'true'" />
      <xsl:when test="$paramValue = $value">
	<a rel="nofollow" title="remove filter">
	  <xsl:attribute name="href">
	    <xsl:call-template name="substituteParam">
	      <xsl:with-param name="uri">
		<xsl:apply-templates select="/result" mode="searchURI" />
	      </xsl:with-param>
	      <xsl:with-param name="param" select="$paramName" />
	      <xsl:with-param name="value" select="''" />
	    </xsl:call-template>
	  </xsl:attribute>
	  <img src="{$activeImageBase}/Back.png" alt="remove filter" />
	</a>
      </xsl:when>
      <xsl:when test="$datatype = 'integer' or $datatype = 'decimal'
                      or $datatype = 'float' or $datatype = 'int' or
                      $datatype = 'date' or $datatype = 'dateTime' or
                      $datatype = 'time'">
	<xsl:variable name="min">
	  <xsl:call-template name="paramValue">
	    <xsl:with-param name="uri">
	      <xsl:apply-templates select="/result" mode="searchURI" />
	    </xsl:with-param>
	    <xsl:with-param name="param" select="concat('min-',
                                                 $paramName)" />
	  </xsl:call-template>
	</xsl:variable>
	<xsl:variable name="max">
	  <xsl:call-template name="paramValue">
	    <xsl:with-param name="uri">
	      <xsl:apply-templates select="/result" mode="searchURI" />
	    </xsl:with-param>
	    <xsl:with-param name="param" select="concat('max-',
                                                 $paramName)" />
	  </xsl:call-template>
	</xsl:variable>
	<xsl:choose>
	  <xsl:when test="$max = $value">
	    <a rel="nofollow" title="remove maximum value filter">
	      <xsl:attribute name="href">
		<xsl:call-template name="substituteParam">
		  <xsl:with-param name="uri">
		    <xsl:apply-templates select="/result" mode="searchURI" />
		  </xsl:with-param>
		  <xsl:with-param name="param" select="concat('max-',
                                                       $paramName)" />
		  <xsl:with-param name="value" select="''" />
		</xsl:call-template>
	      </xsl:attribute>
	      <img src="{$activeImageBase}/Back.png"
                   alt="remove maximum value filter" />
	    </a>
	  </xsl:when>
	  <xsl:otherwise>
	    <a rel="nofollow" title="filter to values equal to or less than {$value}">
	      <xsl:attribute name="href">
		<xsl:call-template name="substituteParam">
		  <xsl:with-param name="uri">
		    <xsl:apply-templates select="/result" mode="searchURI" />
		  </xsl:with-param>
		  <xsl:with-param name="param" select="concat('max-',
                                                       $paramName)" />
		  <xsl:with-param name="value" select="$value" />
		</xsl:call-template>
	      </xsl:attribute>
	      <xsl:choose>
		<xsl:when test="$max != ''">
		  <img src="{$activeImageBase}/Arrow3_Left.png"
                       alt="equal to or less than {$value}" />
		</xsl:when>
		<xsl:otherwise>
		  <img src="{$inactiveImageBase}/Arrow3_Left.png"
                       alt="equal to or less than {$value}" />
		</xsl:otherwise>
	      </xsl:choose>
	    </a>
	  </xsl:otherwise>
	</xsl:choose>
	<xsl:choose>
	  <xsl:when test="$min = $value">
	    <a rel="nofollow" title="remove minimum value filter">
	      <xsl:attribute name="href">
		<xsl:call-template name="substituteParam">
		  <xsl:with-param name="uri">
		    <xsl:apply-templates select="/result"
                                         mode="searchURI" />
		  </xsl:with-param>
		  <xsl:with-param name="param" select="concat('min-',
                                                       $paramName)" />
		  <xsl:with-param name="value" select="''" />
		</xsl:call-template>
	      </xsl:attribute>
	      <img src="{$activeImageBase}/Back.png"
                   alt="remove minimum value filter" />
	    </a>
	  </xsl:when>
	  <xsl:otherwise>
	    <a rel="nofollow" title="filter to values equal to or more than {$value}">
	      <xsl:attribute name="href">
		<xsl:call-template name="substituteParam">
		  <xsl:with-param name="uri">
		    <xsl:apply-templates select="/result"
                                         mode="searchURI" />
		  </xsl:with-param>
		  <xsl:with-param name="param" select="concat('min-',
                                                       $paramName)" />
		  <xsl:with-param name="value" select="$value" />
		</xsl:call-template>
	      </xsl:attribute>
	      <xsl:choose>
		<xsl:when test="$min != ''">
		  <img src="{$activeImageBase}/Arrow3_Right.png"
                       alt="equal to or more than {$value}" />
		</xsl:when>
		<xsl:otherwise>
		  <img src="{$inactiveImageBase}/Arrow3_Right.png"
                       alt="equal to or more than {$value}" />
		</xsl:otherwise>
	      </xsl:choose>
	    </a>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>
      <xsl:otherwise>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Patch 6 -->
  <!-- Fix display of format labels. The link "html"
       did not appear properly, and caused wrapping. -->
  <xsl:template match="hasFormat/item" mode="nav">
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="format">
          <xsl:apply-templates select="." mode="name" />
        </xsl:when>
        <!-- pick the (misplaced) label up from the result if there is one there -->
<!--            <xsl:when test="/result/label"> -->
<!--               <xsl:apply-templates select="/result" mode="name" /> -->
<!--            </xsl:when> -->
           <!-- pick up best label from misplaced result - cann only happen for html-->
           <xsl:otherwise>
             <xsl:text>html</xsl:text>
<!--               <xsl:apply-templates select="/result" mode="name" /> -->
           </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="format">
        <a href="{@href}" type="{format/label}" rel="alternate"
           title="view in {$name} format">
          <xsl:value-of select="label" />
        </a>
      </xsl:when>
      <xsl:when test="/result/format/label and /result/label">
        <a href="{@href}" type="{/result/format/label}" rel="alternate"
           title="view in {$name} format">
<!--                <xsl:value-of select="/result/label" /> -->
          <xsl:value-of select="$name" />
        </a>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Patch 7 -->
  <!-- The off-the-shelf searchURI template assumes that there is
       a "top-level" list endpoint whose URL can be obtained
       by stripping back the URL of the current page, removing
       everything after the last slash. But that's wrong for
       SISSVoc; for the item endpoint whose URL ends in
       .../resource, we want to switch to the list endpoint
       .../concept.

       So, if the current URL is the _item_ endpoint .../resource,
       replace it with the concept _list_ endpoint .../concept, and
       remove any ?uri=... parameter.

       This template is made simpler than the off-the-shelf version
       that uses the "uriExceptLastPart" template, as we take
       advantage of Saxon's nice fn:replace() function.

       Structure of the template:
       * switch on whether this is a _list_ endpoint.
       * if it is a list endpoint, use the href of the first result.
       * otherwise (it's an _item_ endpoint), start with the href, and
         see if it contains the resourceEndPoint. If so, match on it, as
         well as any extension (e.g., ".html" or ".json") and any
         "uri=..." parameter. Replace the resourceEndPoint with
         conceptSearchEndPoint, and remove any "uri=..." parameter,
         leaving the extension and any other query parameters instact.
  -->
  <xsl:param name="conceptSearchEndPoint" />

  <xsl:template match="result" mode="searchURI">
    <xsl:choose>
      <xsl:when test="items">
        <xsl:value-of select="first/@href" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="searchURIoriginal" select="@href" />
        <xsl:variable name="searchURIafterReplace">
          <xsl:choose>
            <xsl:when test="contains($searchURIoriginal, $resourceEndPoint)">
              <xsl:value-of select="fn:replace($searchURIoriginal,
                                      concat($resourceEndPoint,'(\.[a-z]+)?(\?uri=[^&amp;]+)?&amp;?'),
                                      concat($conceptSearchEndPoint,'$1'))"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$searchURIoriginal" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="$searchURIafterReplace" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Patch 8 -->
  <!-- _Don't_ use the off-the-shelf ambiguous formatting 06/06/2018,
       but use YYYY-MM-DD.
       _Do_ keep the pretty-printing of replacing the "T" with a space.
       _Don't_ trim the timezone.
  -->
  <xsl:template match="*[@datatype = 'date' or @datatype = 'dateTime' or @datatype = 'time']" mode="display">
    <time datetime="{.}">
      <xsl:choose>
        <xsl:when test="@datatype = 'date' or @datatype = 'dateTime'">
          <xsl:value-of select="substring(., 1, 10)" />
          <xsl:if test="@datatype = 'dateTime'">
            <xsl:text> </xsl:text>
            <xsl:value-of select="substring(substring-after(., 'T'), 1)" />
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="content" />
        </xsl:otherwise>
      </xsl:choose>
    </time>
  </xsl:template>

</xsl:stylesheet>
