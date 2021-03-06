import uk.gov.hmcts.slack.SlackChannelRetriever

/**
 * Send build notification
 * <p>
 * When build happens on master branch then specified team channel is notified. In other cases change author is notified instead.
 * <p>
 * When change author does not have Slack account or uses other Slack username than one registered in LDAP then notification won't be sent.
 *
 * @param args arguments:
 *  <ul>
 *      <li>channel - (string; required) name of the slack channel for team notifications</li>
 *      <li>color   - (string) color of the notification. Valid options are good, danger or warning.</li>
 *      <li>message - (string) message to send. Default is Build has FAILED.</li>
 *  </ul>
 */
def call(Map args = [:]) {
  def config = [
    color: 'danger',
    message: 'has FAILED'
  ] << args

  validate(config)

  String changeAuthor = env.CHANGE_AUTHOR

  String channel
  if (env.BRANCH_NAME == 'master') {
    channel = args.channel
  } else {
    channel = new SlackChannelRetriever(this).retrieve(args.channel as String, changeAuthor)
  }

  try {
    slackSend(
        channel: channel,
        color: config.color,
        message: "${env.JOB_NAME}: <${env.RUN_DISPLAY_URL}|Build ${env.BUILD_DISPLAY_NAME}> ${config.message}")
  } catch (Exception ex) {
    echo "ERROR: Failed to notify ${channel} due to the following error: ${ex}"
  }
}

private static validate(Map args) {
  if (args.channel == null) throw new Exception('Slack channel is required')
  if (!(args.color =~ /^(good|danger|warning)$/)) throw new Exception('A color is required (good|danger|warning)')
}


