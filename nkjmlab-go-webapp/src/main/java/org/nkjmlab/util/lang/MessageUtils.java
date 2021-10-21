package org.nkjmlab.util.lang;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;

public class MessageUtils {

  public static String format(String msg, Object... params) {
    return ParameterizedMessageFactory.INSTANCE.newMessage(msg, params).getFormattedMessage();
  }

}
