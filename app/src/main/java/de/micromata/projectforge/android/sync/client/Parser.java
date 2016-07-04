package de.micromata.projectforge.android.sync.client;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.http.ParseException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 * Created by lado on 01.07.16.
 */

public class Parser
{
  private JsonParser jParser;

  private int mPhotoDim = 720;

  public Parser()
  {

  }

  private String getString() throws Exception
  {
    return jParser.nextTextValue();
  }

  private boolean getBoolean() throws Exception
  {
    return jParser.nextBooleanValue();
  }

  private long getLong() throws Exception
  {
    //jParser.nextToken();
    return jParser.nextLongValue(-1l);
  }

  public void parseImpl(final ArrayList<RawContact> serverDirtyList, final InputStream is, Context context) throws
      Exception
  {

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      if (context != null) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
            new String[] { ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM }, null, null, null);
        cursor.moveToFirst();
        mPhotoDim = cursor.getInt(0);
        if (cursor != null && cursor.isClosed() == false) {
          cursor.close();
        }
      }
    }


    JsonFactory jfactory = new JsonFactory();

    /*** read from file ***/
    jParser = jfactory.createJsonParser(is);
    JsonToken current = jParser.nextToken();
    if (current != JsonToken.START_ARRAY) {
      System.err.println("Not an array");
    }
    // loop until token equal to "}"
    RawContact c = null;
    while (true) {
      current = jParser.nextToken();
      if (current == JsonToken.START_OBJECT) {
        c = new RawContact();
        c.setAddr(new RawAddress());
        c.setPrivateAddr(new RawAddress());
        c.setPostalAddr(new RawAddress());
        Log.i(Parser.class.getSimpleName(), "create RawContact");
        continue;
      }

      if (current == JsonToken.END_OBJECT) {
        c.finalize();
        serverDirtyList.add(c);
        Log.i(Parser.class.getSimpleName(), "finalize RawContact, current size: " + serverDirtyList.size());
        continue;
      }

      if (current == JsonToken.END_ARRAY) {
        Log.i(Parser.class.getSimpleName(), "Array End reached: " + serverDirtyList.size());
        break;
      }

      String fieldname = jParser.getCurrentName();

      if (fieldname.equals("contactStatus") == true) {
        String contactStatus = getString();
        c.setContactStatus(contactStatus);
      } else if ("firstName".equals(fieldname)) {
        c.setFirstName(getString());
      } else if (fieldname.equals("name")) {
        String text = getString();
        c.setLastName(text);
      } else if (fieldname.equals("privateEmail")) {
        c.setHomeEmail(getString());
      } else if (fieldname.equals("email")) {
        c.setWorkEmail(getString());
      } else if (fieldname.equals("privateMobilePhone")) {
        c.setHomeMobilePhone(getString());
      } else if (fieldname.equals("mobilePhone")) {
        c.setWorkMobilePhone(getString());
      } else if (fieldname.equals("privatePhone")) {
        c.setHomePhone(getString());
      } else if (fieldname.equals("businessPhone")) {
        c.setWorkPhone(getString());
      } else if (fieldname.equals("fax")) {
        c.setWorkFax(getString());
      } else if (fieldname.equals("organization")) {
        c.setCompany(getString());
      } else if (fieldname.equals("division")) {
        c.setDivision(getString());
      } else if (fieldname.equals("positionText")) {
        c.setPosition(getString());
      } else if (fieldname.equals("website")) {
        String website = getString();
        c.setWebsite(website);
      } else if (fieldname.equals("comment")) {
        c.setNote(getString());
      } else if (fieldname.equals("addressStatus")) {
        String addressStatus = getString();
        c.setAddressStatus(addressStatus);
      } else if (fieldname.equals("form")) {
        c.setForm(getString());
      } else if (fieldname.equals("addressText")) {
        c.getAddr().setAddressText(getString());
      } else if (fieldname.equals("zipCode")) {
        c.getAddr().setZipCode(getString());
      } else if (fieldname.equals("city")) {
        c.getAddr().setCity(getString());
      } else if (fieldname.equals("state")) {
        c.getAddr().setState(getString());
      } else if (fieldname.equals("country")) {
        c.getAddr().setCountry(getString());
      } else if (fieldname.equals("postalAddressText")) {
        c.getPostalAddr().setAddressText(getString());
      } else if (fieldname.equals("postalZipCode")) {
        c.getPostalAddr().setZipCode(getString());
      } else if (fieldname.equals("postalCity")) {
        c.getPostalAddr().setCity(getString());
      } else if (fieldname.equals("postalState")) {
        c.getPostalAddr().setState(getString());
      } else if (fieldname.equals("postalCountry")) {
        c.getPostalAddr().setCountry(getString());
      } else if (fieldname.equals("privateAddressText")) {
        c.getPrivateAddr().setAddressText(getString());
      } else if (fieldname.equals("privateZipCode")) {
        c.getPrivateAddr().setZipCode(getString());
      } else if (fieldname.equals("privateCity")) {
        c.getPrivateAddr().setCity(getString());
      } else if (fieldname.equals("privateState")) {
        c.getPrivateAddr().setState(getString());
      } else if (fieldname.equals("privateCountry")) {
        c.getPrivateAddr().setCountry(getString());
      } else if (fieldname.equals("id")) {
        c.setServerContactId(getLong());
      } else if (fieldname.equals("image")) {
        current = jParser.nextToken();
        ArrayList<Integer> bytes = new ArrayList<Integer>();
        while ((current = jParser.nextToken()) != JsonToken.END_ARRAY) {
          bytes.add(jParser.getIntValue());
        }
        byte[] data = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
          final int integer = bytes.get(i);
          data[i] = (byte) integer;
        }
        bytes.clear();
        c.setAvatar(data);
        //Log.i(Parser.class.getSimpleName(), "skipping image:");
        ////jParser.skipChildren();
      } else if (fieldname.equals("communicationLanguage")) {
        c.setCommunicationLanguage(getString());
      } else if (fieldname.equals("publicKey")) {
        c.setPublicKey(getString());
      } else if (fieldname.equals("deleted")) {
        c.setDeleted(getBoolean());
      } else if (fieldname.equals("lastUpdate")) {
        c.setLastUpdate(getString());
      } else {
        //System.err.println("missed: " + fieldname);
        getString();
      }


      //      if ("age".equals(fieldname)) {
      //
      //        // current token is "age",
      //        // move to next, which is "name"'s value
      //        jParser.nextToken();
      //        System.out.println(jParser.getIntValue()); // display 29
      //
      //      }
      //
      //      if ("messages".equals(fieldname)) {
      //
      //        jParser.nextToken(); // current token is "[", move next
      //
      //        // messages is array, loop until token equal to "]"
      //        while (jParser.nextToken() != JsonToken.END_ARRAY) {
      //
      //          // display msg1, msg2, msg3
      //          System.out.println(jParser.getText());
      //
      //        }
      //
      //      }

    }
    jParser.close();
  }

  public void parse(final ArrayList<RawContact> serverDirtyList, final InputStream is, Context context)
  {
    try {
      parseImpl(serverDirtyList, is, context);
    } catch (Exception e) {
      Log.e(Parser.class.getSimpleName(), "Crash", e);
      throw new ParseException(e.getMessage());
    }

  }
}
