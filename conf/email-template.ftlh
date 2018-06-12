<#-- Generate report for one vocabulary. -->
<#macro reportVocabulary vocabId>
    <#assign vdiff = vocabularyIdMap?api.get(vocabId)>
    <h2><#if vdiff.finalResult != "DELETED">
        <a href="${properties["Notifications.portalPrefix"]}viewById/${vocabId}"
           target="_blank">${vdiff.title}</a>
    <#else>${vdiff.title}</#if>
        <#if vdiff.finalResult == "CREATED">
            <b>New</b>
        </#if>
    </h2>

    <#list vdiff.vocabularyDiffs>
        <ul>
            <#items as vocabularyDiff>
                <li>${vocabularyDiff}</li>
            </#items>
        </ul>
    </#list>

    <#if vdiff.finalResult == "UPDATED">
        <#list vdiff.fieldDiffs>
            The following descriptive metadata elements were updated:
            <ul>
                <#items as fieldDiff>
                    <li>${fieldDiff.fieldName}</li>
                </#items>
            </ul>
        </#list>

        <#list vdiff.versionDiffs as versionId, verDiff>
            hello
            <#-- Report on a version if the finalResult is
              either CREATED or DELETED, or, if it is UPDATED,
              there is either a versionDiff or a fieldDiff. -->
            <#if verDiff.finalResult != "UPDATED"
            || verDiff.versionDiffs?has_content
            || verDiff.fieldDiffs?has_content>
                <h4>${verDiff.title}</h4>
                <p>Final result: ${verDiff.finalResult}</p>
                <#list verDiff.versionDiffs>
                    <ul>
                        <#items as versionDiff>
                            <li>${versionDiff}</li>
                        </#items>
                    </ul>
                </#list>
                <#list verDiff.fieldDiffs>
                    <ul>
                        <#items as fieldDiff>
                            <li>${fieldDiff.fieldName}</li>
                        </#items>
                    </ul>
                </#list>
            </#if>
        </#list>

    </#if>
</#macro>


<h1>Research Vocabularies Australia Weekly Digest</h1>

<#-- Reports for individual vocabularies -->
<#list allIndividualVocabularySubscriptions>
    <h2>Changes to vocabularies you are subscribed to</h2>

    <#items as vocabId>
        <@reportVocabulary vocabId />
    </#items>
</#list>

<#-- Reports grouped by owner -->
<#list allOwnerIdsToReport as ownerId>
    <h1>New/changed vocabularies from ${ownerFullNames?api.get(ownerId)}</h1>

    <#list ownerVocabularies?api.get(ownerId) as vocabId>
        <@reportVocabulary vocabId />
    </#list>
</#list>

<p><a href="${properties["Notifications.portalPrefix"]}vocabs/manageSubscriptions/${token}"
   target="_blank">Manage your subscription preferences</a></p>

<p>This is an automated email; please do not reply. For more information,
    ideas for improvements, or issues using the service, email
    <a href="mailto:services@ands.org.au">services@ands.org.au</a>.</p>

<p><a href="${properties["Notifications.portalPrefix"]}"
      target="_blank">Research Vocabularies Australia</a>
    is provided by the
    <a href="http://ands.ands.org.au/"
       target="_blank">Australian National Data Service</a>
    in partnership with
    <a href="https://www.ands-nectar-rds.org.au/"
       target="_blank">Nectar and RDS</a>.</p>
