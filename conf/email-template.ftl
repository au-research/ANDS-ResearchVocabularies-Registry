<h1>Research Vocabularies Australia Weekly Digest</h1>

<h2>Changes to vocabularies you are subscribed to</h2>

<#list allIndividualVocabularySubscriptions as vocabId>
    <#assign vdiff = vocabularyIdMap?api.get(vocabId)>
    <a>${vdiff.title}</a>    <a>unsubscribe</a>

    <#if vdiff.fieldDiffs?? \and vdiff.fieldDiffs.numberOfDiffs gt 0>
        The following descriptive metadata elements were updated:

        <ul>
        </ul>
    </#if>

    Version: .title

    <ul>
    </ul>
</#list>

<#list allOwnerIdsToReport as ownerId>
    <h2>New/changed vocabularies from ${ownerFullNames[ownerId]}</h2>
    <a>unsubscribe</a>

    <#list ownerVocabularies?api.get(ownerId) as vocabId>
        <#assign vdiff = vocabularyIdMap?api.get(vocabId)>
        <a>${vdiff.title}</a> <b>New</b>

        The following descriptive metadata elements were updated:

    </#list>

</#list>
