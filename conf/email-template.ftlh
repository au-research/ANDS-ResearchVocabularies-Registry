<#-- Generate report for one vocabulary. -->
<#macro reportVocabulary vocabId>
    <#assign vdiff = vocabularyIdMap?api.get(vocabId)>
    <h3><#if vdiff.finalResult != "DELETED">
        <a href="${properties["Notifications.portalPrefix"]}viewById/${vocabId?c}"
           target="_blank">${vdiff.title}</a>
    <#else>${vdiff.title}</#if>
        <#if vdiff.finalResult == "CREATED">
            <b>* New *</b>
        </#if>
    </h3>

    <#list vdiff.vocabularyDiffs>
        <ul>
            <#items as vocabularyDiff>
                <li>${vocabularyDiff}</li>
            </#items>
        </ul>
    </#list>

    <#if vdiff.finalResult == "UPDATED">
        <#list vdiff.fieldDiffs>
            <p>The following descriptive metadata elements were updated:</p>
            <ul>
                <#items as fieldDiff>
                    <li>${fieldDiff.fieldName?cap_first}</li>
                </#items>
            </ul>
        </#list>

        <#list vdiff.versionDiffs>
            <p>The following version changes were made:</p>

            <div style="padding-left: 30px">
                <#items as versionId, verDiff>
                    <#-- Report on a version if the finalResult is
                      either CREATED or DELETED, or, if it is UPDATED,
                      there is either a versionDiff or a fieldDiff. -->
                    <#if verDiff.finalResult != "UPDATED"
                    || verDiff.versionDiffs?has_content
                    || verDiff.fieldDiffs?has_content>
                        <p style="margin-bottom: 5px">${verDiff.title}</p>
                        <ul style="margin-top: 5px">
                            <#list verDiff.versionDiffs>
                                <#items as versionDiff>
                                    <li>${versionDiff}</li>
                                </#items>
                            </#list>
                            <#list verDiff.fieldDiffs>
                                <#items as fieldDiff>
                                    <li>${fieldDiff.fieldName?cap_first}
                                        updated</li>
                                </#items>
                            </#list>
                        </ul>
                    </#if>
                </#items>
            </div>
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
    <h2>New/changed vocabularies from ${ownerFullNames?api.get(ownerId)}</h2>

    <#list ownerVocabularies?api.get(ownerId) as vocabId>
        <@reportVocabulary vocabId />
    </#list>
</#list>

<div style="text-align: center">
    <p>
        <a href="${properties["Notifications.portalPrefix"]}vocabs/manageSubscriptions/${token}"
           target="_blank"
           style="text-decoration: none;">
            <button type="button"
                    style="cursor: pointer; padding-left: 50px; padding-right: 50px; font-size: 125%; color: black; background-color: LightGray">
                Manage your subscription preferences
            </button>
        </a>
    </p>

    <p>This is an automated email; please do not reply. For more information,
        ideas for improvements, or issues using the service, email
        <a href="mailto:services@ardc.edu.au">services@ardc.edu.au</a>.</p>

    <p><a href="${properties["Notifications.portalPrefix"]}"
          target="_blank">Research Vocabularies Australia</a>
        is provided by the
        <a href="https://ardc.edu.au/"
           target="_blank">Australian Research Data Commons</a>.</p>
</div>
