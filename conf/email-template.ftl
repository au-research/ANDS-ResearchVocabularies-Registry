<#-- Generate report for one vocabulary. -->
<#macro reportVocabulary vocabId>
    <#assign vdiff = vocabularyIdMap?api.get(vocabId)>
    <#if vdiff.finalResult != "DELETED">
           ${vdiff.title}
        <#if vdiff.finalResult == "CREATED">
            * New *
        </#if>
        ${properties["Notifications.portalPrefix"]}viewById/${vocabId}
    <#else>${vdiff.title}</#if>

    <#list vdiff.vocabularyDiffs>
        <#items as vocabularyDiff>
             * ${vocabularyDiff}
        </#items>
    </#list>

    <#if vdiff.finalResult == "UPDATED">
        <#list vdiff.fieldDiffs>
            <p>The following descriptive metadata elements were updated:</p>
            <ul>
                <#items as fieldDiff>
                    <li>${fieldDiff.fieldName}</li>
                </#items>
            </ul>
        </#list>

        <#list vdiff.versionDiffs>
            <p>The following version changes were made:</p>

            <#items as versionId, verDiff>
                <#-- Report on a version if the finalResult is
                  either CREATED or DELETED, or, if it is UPDATED,
                  there is either a versionDiff or a fieldDiff. -->
                <#if verDiff.finalResult != "UPDATED"
                || verDiff.versionDiffs?has_content
                || verDiff.fieldDiffs?has_content>
                    <h4>${verDiff.title}</h4>
                    <#if verDiff.finalResult == "UPDATED">
                        <ul>
                    </#if>
                    <#list verDiff.versionDiffs>
                        <#items as versionDiff>
                            <li>${versionDiff}</li>
                        </#items>
                    </#list>
                    <#list verDiff.fieldDiffs>
                        <#items as fieldDiff>
                            <li>${fieldDiff.fieldName?capitalize} updated</li>
                        </#items>
                    </#list>
                    <#if verDiff.finalResult == "UPDATED">
                        </ul>
                    </#if>
                </#if>
            </#items>
        </#list>

    </#if>
</#macro>


Research Vocabularies Australia Weekly Digest

<#-- Reports for individual vocabularies -->
<#list allIndividualVocabularySubscriptions>
Changes to vocabularies you are subscribed to

    <#items as vocabId>
        <@reportVocabulary vocabId />
    </#items>
</#list>

<#-- Reports grouped by owner -->
<#list allOwnerIdsToReport as ownerId>
    New/changed vocabularies from ${ownerFullNames?api.get(ownerId)}

    <#list ownerVocabularies?api.get(ownerId) as vocabId>
        <@reportVocabulary vocabId />
    </#list>
</#list>

Manage your subscription preferences
${properties["Notifications.portalPrefix"]}vocabs/manageSubscriptions/${token}

This is an automated email; please do not reply. For more information,
ideas for improvements, or issues using the service, email
services@ands.org.au.

Research Vocabularies Australia is provided by the Australian National
Data Service in partnership with Nectar and RDS.
${properties["Notifications.portalPrefix"]}
http://ands.org.au/
