/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification.email;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.SubscriberEmailAddressDAO;
import au.org.ands.vocabs.registry.db.entity.Subscriber;
import au.org.ands.vocabs.registry.db.entity.SubscriberEmailAddress;
import au.org.ands.vocabs.registry.notification.CollectEvents;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/** Generate and send email notifications. */
public final class SendEmailNotifications {

    /** Logger for this class. */
    private static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /** Private constructor for a utility class. */
    private SendEmailNotifications() {
    }

    /** FreeMarker configuration. Created and initialized by
     * {@link #configureFreeMarker()}.
     */
    private static Configuration freeMarkerCfg;

    /** The plain text version of the email template. */
    private static Template templatePlaintext;

    /** The HTML version of the email template. */
    private static Template templateHTML;

    /** Map of system properties needed. */
    private static Map<String, String> properties = new HashMap<>();

    /** Configure the map of system properties needed by the templates. */
    private static void configureSystemProperties() {
        String[] propertyNames = {
                PropertyConstants.NOTIFICATIONS_PORTAL_PREFIX,
        };
        for (String propertyName : propertyNames) {
            properties.put(propertyName,
                    RegistryProperties.getProperty(propertyName));
        }
    }

    /** Create and configure the FreeMarker configuration. */
    private static void configureFreeMarker() {
        Version freeMarkerVersion = Configuration.VERSION_2_3_28;
        freeMarkerCfg = new Configuration(freeMarkerVersion);
        // Configure "iterable support", in particular, to support
        // using DiffResult as an Iterable. Not needed for now.
//        DefaultObjectWrapperBuilder owb = new DefaultObjectWrapperBuilder(
//                freeMarkerVersion);
//        owb.setIterableSupport(true);
//        freeMarkerCfg.setObjectWrapper(owb.build());

        try {
//            freeMarkerCfg.setDirectoryForTemplateLoading(
//                    new File("/where/you/store/templates"));
            freeMarkerCfg.setDirectoryForTemplateLoading(
                    new File("."));
        } catch (IOException e) {
            logger.error("Unable to set directory for template loading", e);
        }
        freeMarkerCfg.setDefaultEncoding("UTF-8");
        freeMarkerCfg.setTemplateExceptionHandler(
                TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarkerCfg.setLogTemplateExceptions(false);
        freeMarkerCfg.setWrapUncheckedExceptions(true);
        // We use ?api.get() on Maps, so we need to enable ?api.
        freeMarkerCfg.setAPIBuiltinEnabled(true);
        try {
            templatePlaintext = freeMarkerCfg.getTemplate(
                    RegistryProperties.getProperty(
                            PropertyConstants.
                            NOTIFICATIONS_EMAIL_TEMPLATE_PLAINTEXT));
            templateHTML = freeMarkerCfg.getTemplate(
                    RegistryProperties.getProperty(
                            PropertyConstants.
                            NOTIFICATIONS_EMAIL_TEMPLATE_HTML));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** The subscribers with current email subscriptions.
     * Keys are subscriber Ids. */
    private static Map<Integer, Subscriber> subscribersMap;

    /** Map of subscribers to the unified model of their subscriptions.
     * Keys are subscriber Ids.
     */
    private static Map<Integer, SubscriberSubscriptionsModel>
        subscriberSubscriptionsModels;

    /** Collect the data needed for all the notification emails that are
     * to be sent out.
     * @param startDate The start date to use for the notification period.
     */
    private static void collectNotificationData(final LocalDateTime startDate) {
        CollectEvents collectedEvents = new CollectEvents(startDate);
        logger.info("Number of VocabularyDifferences computed: "
                + collectedEvents.getVocabularyIdMap().size());
        CollectSubscriptions collectSubscriptions = new CollectSubscriptions();
        collectSubscriptions.computeVocabularySubscriptionsForSubscribers(
                collectedEvents);
        subscribersMap = collectSubscriptions.getSubscribersMap();
        subscriberSubscriptionsModels =
                collectSubscriptions.getSubscriberSubscriptionsModels();
    }

    /** Generate and send the notification emails.
     */
    private static void sendEmails() {
        for (Entry<Integer, SubscriberSubscriptionsModel> modelEntry
                : subscriberSubscriptionsModels.entrySet()) {
            Integer subscriberId = modelEntry.getKey();
//            Subscriber subscriber = subscribersMap.get(subscriberId);
            List<SubscriberEmailAddress> seaList = SubscriberEmailAddressDAO.
                    getCurrentSubscriberEmailAddressListForSubscriber(
                            subscriberId);
            if (seaList.isEmpty()) {
                logger.error("No current email address recorded for "
                        + "subscriber; subscriber Id: " + subscriberId);
            }
            SubscriberSubscriptionsModel model = modelEntry.getValue();
            if (model.isEmpty()) {
                // Nothing to notify for this subscriber.
                logger.info("Nothing to report for subscriber: "
                        + subscriberId);
                continue;
            }
            // Send to all recorded email addresses for the subscriber.
            for (SubscriberEmailAddress sea : seaList) {
                String emailAddress = sea.getEmailAddress();
                logger.info("Email for address: " + emailAddress);
                // Set system properties, as they weren't set during
                // model generation.
                model.setProperties(properties);
                Map<Integer, String> ownerFullNames =
                        model.getOwnerFullNames();
                for (Entry<Integer, String> ofnEntry
                        : ownerFullNames.entrySet()) {
                    logger.info("ofn: id: " + ofnEntry.getKey()
                        + "; name: " + ofnEntry.getValue());
                }
                Writer out = new OutputStreamWriter(System.out);
                try {
                    templateHTML.process(modelEntry.getValue(), out);
                } catch (TemplateException e) {
                    logger.error("Exception processing template", e);
                    continue;
                } catch (IOException e) {
                    logger.error("Exception writing output", e);
                    continue;
                }

            }
        }
    }

    /** Main method.
     * @param args Command-line arguments.
     */
    public static void main(final String[] args) {
        configureSystemProperties();
        configureFreeMarker();
        LocalDateTime startDate = TemporalUtils.nowUTC().minusWeeks(1);
        logger.info("Notification start date: " + startDate);
        collectNotificationData(startDate);
        logger.info("Number of subscriber/subscription models: "
                + subscriberSubscriptionsModels.size());
        sendEmails();
    }

}
