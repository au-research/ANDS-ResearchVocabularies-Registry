<#-- Generate report for one vocabulary. -->
<#macro reportVocabulary vocabId>
<#assign vdiff = vocabularyIdMap?api.get(vocabId)>
<#if vdiff.finalResult != "DELETED">
<#if vdiff.finalResult == "CREATED">** New </#if>** ${vdiff.title}
   ${properties["Notifications.portalPrefix"]}viewById/${vocabId}
<#else>
${vdiff.title}
</#if>

<#list vdiff.vocabularyDiffs>
 <#items as vocabularyDiff>
   - ${vocabularyDiff}
 </#items>

</#list>
<#if vdiff.finalResult == "UPDATED">
<#list vdiff.fieldDiffs>
   The following descriptive metadata elements were updated:
   <#items as fieldDiff>
   - ${fieldDiff.fieldName?capitalize}
   </#items>

</#list>
<#list vdiff.versionDiffs>
   The following version changes were made:
<#items as versionId, verDiff>
 <#-- Report on a version if the finalResult is
      either CREATED or DELETED, or, if it is UPDATED,
      there is either a versionDiff or a fieldDiff. -->
 <#if verDiff.finalResult != "UPDATED"
      || verDiff.versionDiffs?has_content
      || verDiff.fieldDiffs?has_content>

   ${verDiff.title}
   <#list verDiff.versionDiffs as versionDiff>
   - ${versionDiff}
   </#list>
   <#list verDiff.fieldDiffs as fieldDiff>
   - ${fieldDiff.fieldName?capitalize} updated
   </#list>
 </#if>
</#items>
</#list>

</#if>
</#macro>
=============================================
Research Vocabularies Australia Weekly Digest
=============================================

<#-- Reports for individual vocabularies -->
<#list allIndividualVocabularySubscriptions>
Changes to vocabularies you are subscribed to
---------------------------------------------

<#items as vocabId>
<@reportVocabulary vocabId />
</#items>
</#list>

<#-- Reports grouped by owner -->
<#list allOwnerIdsToReport as ownerId>
New/changed vocabularies from ${ownerFullNames?api.get(ownerId)}
------------------------------${""?left_pad(ownerFullNames?api.get(ownerId)?length, "-")}

<#list ownerVocabularies?api.get(ownerId) as vocabId>
<@reportVocabulary vocabId />
</#list>
</#list>

++ Manage your subscription preferences
   ${properties["Notifications.portalPrefix"]}vocabs/manageSubscriptions/${token}

This is an automated email; please do not reply. For more information,
ideas for improvements, or issues using the service, email
services@ands.org.au.

Research Vocabularies Australia is provided by
the Australian National Data Service.
${properties["Notifications.portalPrefix"]}
https://ands.org.au/
