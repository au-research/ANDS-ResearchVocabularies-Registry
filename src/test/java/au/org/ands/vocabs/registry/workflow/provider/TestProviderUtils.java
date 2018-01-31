/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.harvest.PoolPartyHarvestProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;

/** Tests of the ProviderUtils class. */
public class TestProviderUtils {

    /** Run tests of the {@link ProviderUtils#providerName(Class)} method. */
    @Test
    public void testProviderName() {

        Assert.assertEquals(ProviderUtils.providerName(
                PoolPartyHarvestProvider.class), "PoolParty");

        Assert.assertEquals(ProviderUtils.providerName(
                JsonListTransformProvider.class), "JsonList");
    }

    /** Run tests of the
     * {@link ProviderUtils#getProvider(SubtaskProviderType, String)}
     * method. */
    @Test
    public void testGetProvider() {

        Assert.assertTrue(ProviderUtils.getProvider(
                SubtaskProviderType.HARVEST, "PoolParty")
                instanceof PoolPartyHarvestProvider,
                "Not an instance of PoolPartyHarvestProvider");

        Assert.assertTrue(ProviderUtils.getProvider(
                SubtaskProviderType.TRANSFORM, "JsonList")
                instanceof JsonListTransformProvider,
                "Not an instance of JsonListTransformProvider");
    }

}
