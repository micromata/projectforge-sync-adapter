package de.micromata.projectforge.android.sync.client;

import static org.junit.Assert.*;

import android.widget.ArrayAdapter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lado on 02.07.16.
 */
public class ParserTest
{
  @org.junit.Test
  public void parse() throws Exception
  {

    ArrayList<RawContact> serverDirtyList = new ArrayList<RawContact>();
    final InputStream is = Parser.class.getClassLoader().getResourceAsStream("dump.json");
    new Parser().parse(serverDirtyList, is, null);
    assertTrue(serverDirtyList.isEmpty() == false);
    System.err.println("size: " + serverDirtyList.size());
  }

}
