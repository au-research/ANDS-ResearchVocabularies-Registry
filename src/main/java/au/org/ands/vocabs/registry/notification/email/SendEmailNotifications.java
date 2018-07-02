/** See the file "LICENSE" for the full license governing this code. */

package au.org.ands.vocabs.registry.notification.email;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ands.vocabs.registry.db.context.DBContext;
import au.org.ands.vocabs.registry.db.context.TemporalUtils;
import au.org.ands.vocabs.registry.db.dao.SubscriberEmailAddressDAO;
import au.org.ands.vocabs.registry.db.entity.SubscriberEmailAddress;
import au.org.ands.vocabs.registry.notification.CollectEvents;
import au.org.ands.vocabs.registry.utils.PropertyConstants;
import au.org.ands.vocabs.registry.utils.RegistryProperties;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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

    /** FreeMarker configuration. Created and initialized by
     * {@link #configureFreeMarker()}.
     */
    private Configuration freeMarkerCfg;

    /** The plain text version of the email template. */
    private Template templatePlaintext;

    /** The HTML version of the email template. */
    private Template templateHTML;

    /** Map of system properties needed. */
    private Map<String, String> properties = new HashMap<>();

    /** Configure the map of system properties needed by the templates. */
    private void configureSystemProperties() {
        String[] propertyNames = {
                PropertyConstants.NOTIFICATIONS_PORTAL_PREFIX,
        };
        for (String propertyName : propertyNames) {
            properties.put(propertyName,
                    RegistryProperties.getProperty(propertyName));
        }
    }

    /** Create and configure the FreeMarker configuration.
     * @throws IOException If there is an error initializing the templates.
     */
    private void configureFreeMarker() throws IOException {
        Version freeMarkerVersion = Configuration.VERSION_2_3_28;

        // We support loading the templates from:
        // * the current directory
        // * the conf directory, if it exists
        // * the directory in the classpath that contains the root package.
        // We do this in order to support invoking this method both
        // from a standalone class and when running within the Registry webapp.
        MultiTemplateLoader mtl;
        if (new File("conf").isDirectory()) {
            FileTemplateLoader ftl1 = new FileTemplateLoader(new File("."));
            FileTemplateLoader ftl2 = new FileTemplateLoader(new File("conf"));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/");
            mtl = new MultiTemplateLoader(
                    new TemplateLoader[] {ftl1, ftl2, ctl});
        } else {
            // We are running within the webapp, and there's no conf
            // directory. Allow loading from the current directory anyway.
            FileTemplateLoader ftl1 = new FileTemplateLoader(new File("."));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/");
            mtl = new MultiTemplateLoader(
                    new TemplateLoader[] {ftl1, ctl});
        }

        freeMarkerCfg = new Configuration(freeMarkerVersion);
        freeMarkerCfg.setTemplateLoader(mtl);
        // Configure "iterable support", in particular, to support
        // using DiffResult as an Iterable. Not needed for now.
//        DefaultObjectWrapperBuilder owb = new DefaultObjectWrapperBuilder(
//                freeMarkerVersion);
//        owb.setIterableSupport(true);
//        freeMarkerCfg.setObjectWrapper(owb.build());

//        try {
//            freeMarkerCfg.setDirectoryForTemplateLoading(
//                    new File("."));
//        } catch (IOException e) {
//            logger.error("Unable to set directory for template loading", e);
//        }
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
            logger.error("Exception while initializing templates", e);
            throw e;
        }
    }

    /** The EntityManager used to fetch and update subscriptions. */
    private EntityManager em;

    /** The transaction in which updates to subscriptions are made. */
    private EntityTransaction txn;

    /** The model of the subscriptions that are being processed.  */
    private CollectSubscriptions collectSubscriptions;

    /** Map of subscribers to the unified model of their subscriptions.
     * Keys are subscriber Ids.
     */
    private Map<Integer, SubscriberSubscriptionsModel>
        subscriberSubscriptionsModels;

    /** Collect the data needed for all the notification emails that are
     * to be sent out.
     * @param startDate The start date to use for the notification period.
     * @param endDate The end date to use for the notification period.
     */
    private void collectNotificationData(final LocalDateTime startDate,
            final LocalDateTime endDate) {
        CollectEvents collectedEvents = new CollectEvents(startDate, endDate);
        logger.info("Number of VocabularyDifferences computed: "
                + collectedEvents.getVocabularyIdMap().size());
        collectSubscriptions = new CollectSubscriptions(em);
        collectSubscriptions.computeVocabularySubscriptionsForSubscribers(
                collectedEvents);
        subscriberSubscriptionsModels =
                collectSubscriptions.getSubscriberSubscriptionsModels();
    }

    /** The hostname of the SMTP server to use. */
    private String smtpHost;

    /** The port number of the SMTP server to use. */
    private int smtpPort;

    /** The email address to use as the sender. */
    private String senderEmailAddress;

    /** The full name to use for the sender .*/
    private String senderFullName;

    /** The email address to use as the reply-to address. */
    private String replyTo;

    /** The beginning of the subject line to use. */
    private String subject;

    /** Get the values of properties used for configuring email sending,
     * and store them in local fields.
     */
    private void configureEmailProperties() {
        smtpHost = RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_SMTPHOST, "localhost");
        smtpPort = Integer.parseInt(RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_SMTPPORT, "25"));
        senderEmailAddress = RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_SENDER_EMAILADDRESS,
                "sender");
        senderFullName = RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_SENDER_FULLNAME);
        replyTo = RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_REPLYTO,
                senderEmailAddress);
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("d MMMM y");
        String formattedDate = localDate.format(formatter);

        subject = RegistryProperties.getProperty(
                PropertyConstants.NOTIFICATIONS_EMAIL_SUBJECT, "").trim()
                + " " + formattedDate;
    }

    /** Generate and send the notification emails.
     */
    private void sendEmails() {
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
//                logger.info("Email for address: " + emailAddress);
                // Set system properties, as they weren't set during
                // model generation.
                model.setProperties(properties);
                Writer htmlWriter = new StringWriter();
                Writer plaintextWriter = new StringWriter();
                try {
                    templateHTML.process(modelEntry.getValue(), htmlWriter);
                    templatePlaintext.process(modelEntry.getValue(),
                            plaintextWriter);
                    String html = htmlWriter.toString();
//                    logger.info("HTML: " + html);
                    String plaintext = plaintextWriter.toString();
//                    logger.info("plaintext: " + plaintext);
                    sendOneEmail(emailAddress, html, plaintext);
                } catch (TemplateException e) {
                    logger.error("Exception processing template", e);
                    continue;
                } catch (IOException e) {
                    logger.error("Exception writing output", e);
                    continue;
                }
            }
            collectSubscriptions.updateLastNotificationForSubscriber(
                    subscriberId, TemporalUtils.nowUTC());
        }
    }

    /** Send one notification email.
     * @param recipient The email address of the intended recipient.
     * @param html The HTML content to include in the email.
     * @param plaintext The plain text content to include in the email.
     */
    private void sendOneEmail(final String recipient,
            final String html, final String plaintext) {
        HtmlEmail email = new HtmlEmail();
        email.setCharset(EmailConstants.UTF_8);
        email.setHostName(smtpHost);
        email.setSmtpPort(smtpPort);
        email.setSubject(subject);
        try {
            email.setFrom(senderEmailAddress, senderFullName);
            email.addReplyTo(replyTo);
            email.addTo(recipient);
            email.setHtmlMsg(html);
            email.setTextMsg(plaintext);
            logger.info("Sending an email to: " + recipient);
            email.send();
        } catch (EmailException e) {
            logger.error("Error configuring or sending email", e);
        }
    }

    /** Main method.
     * @param startDate The start date/time of registry events to consider.
     * @param endDate The end date/time of registry events to consider.
     * @throws IOException If there is an error initializing the templates.
     */
    public void main(final LocalDateTime startDate,
            final LocalDateTime endDate) throws IOException {
        configureSystemProperties();
        configureFreeMarker();
        configureEmailProperties();
        logger.info("Notification start date: " + startDate);
        logger.info("Notification end date: " + endDate);
        try {
            em = DBContext.getEntityManager();
            collectNotificationData(startDate, endDate);
            logger.info("Number of subscriber/subscription models: "
                    + subscriberSubscriptionsModels.size());

            txn = em.getTransaction();
            txn.begin();
            sendEmails();
            txn.commit();
        } catch (Throwable t) {
            if (txn != null && txn.isActive()) {
                try {
                    logger.error("Exception during transaction; rolling back",
                            t);
                    txn.rollback();
                } catch (Exception e) {
                    logger.error("Rollback failure!", e);
                }
            } else {
                logger.error("Exception other than during transaction: ", t);
            }
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /** Main method.
     * @param args Command-line arguments. If no arguments are specified,
     *      the start date is taken as one week ago, and the end date
     *      is taken as now. If one argument is specified,
     *      it is taken as the start date, and the end date is taken as now.
     *      If two arguments are specified, they are used as the start and
     *      end date, respectively.
     * @throws IOException If there is an error initializing the templates.
     */
    public static void main(final String[] args) throws IOException {
        LocalDateTime startDate;
        LocalDateTime endDate;
        switch (args.length) {
        case 0:
            endDate = TemporalUtils.nowUTC();
            startDate = endDate.minusWeeks(1);
            break;
        case 1:
            startDate = LocalDateTime.parse(args[0]);
            endDate = TemporalUtils.nowUTC();
            break;
        case 2:
            startDate = LocalDateTime.parse(args[0]);
            endDate = LocalDateTime.parse(args[1]);
            break;
        default:
            logger.error("Invalid number of arguments: " + args.length);
            return;
        }
        new SendEmailNotifications().main(startDate, endDate);
    }

}
