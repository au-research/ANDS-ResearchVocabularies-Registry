/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.workflow.provider;

import org.testng.Assert;
import org.testng.annotations.Test;

import au.org.ands.vocabs.registry.enums.SubtaskProviderType;
import au.org.ands.vocabs.registry.workflow.provider.backup.PoolPartyBackupProvider;
import au.org.ands.vocabs.registry.workflow.provider.transform.JsonListTransformProvider;

/** Tests of the ProviderUtils class. */
public class TestProviderUtils {

    /** Run tests of the {@link ProviderUtils#providerName(Class)} method. */
    @Test
    public void testProviderName() {

        Assert.assertEquals(ProviderUtils.providerName(
                PoolPartyBackupProvider.class), "PoolParty");

        Assert.assertEquals(ProviderUtils.providerName(
                JsonListTransformProvider.class), "JsonList");
    }

    /** Run tests of the
     * {@link ProviderUtils#getProvider(SubtaskProviderType, String)}
     * method. */
    @Test
    public void testGetProvider() {

        Assert.assertTrue(ProviderUtils.getProvider(
                SubtaskProviderType.BACKUP, "PoolParty")
                instanceof PoolPartyBackupProvider,
                "Not an instance of PoolPartyBackupProvider");

        Assert.assertTrue(ProviderUtils.getProvider(
                SubtaskProviderType.TRANSFORM, "JsonList")
                instanceof JsonListTransformProvider,
                "Not an instance of JsonListTransformProvider");
    }

}
