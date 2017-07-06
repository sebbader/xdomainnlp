package xdomainnlp.noveltydetection.utilities;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.SAXException;

public class FileShortener {
	public void shorten() throws FileNotFoundException, XMLStreamException, SAXException, ParserConfigurationException {
		System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
		System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:\\so-index\\posts_short.txt"), "utf-8"))) {

			InputStream in = new FileInputStream("C:\\so-index\\posts.xml");
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(in);

			int counter = 0;

			while (parser.hasNext()) {
				switching: switch (parser.getEventType()) {
				case XMLStreamConstants.END_DOCUMENT:
					parser.close();
					writer.close();
					break;

				case XMLStreamConstants.START_ELEMENT:
					boolean rightPostTypeId = false;
					boolean rightCreationDate = false;
					String id = "";
					String title = "";
					String body = "";
					String tags = "";

					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeLocalName(i).equals("PostTypeId") && parser.getAttributeValue(i).equals("1")) {
							rightPostTypeId = true;
						}
					}

					if (rightPostTypeId) {
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							if (parser.getAttributeLocalName(i).equals("CreationDate")) {
								if (isRightDate(parser.getAttributeValue(i))) {
									rightCreationDate = true;
								}
							}
						}
					} else {
						break switching;
					}

					if (rightCreationDate) {
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							if (parser.getAttributeLocalName(i).equals("Tags")) {
								tags = applyTagRegEx(parser.getAttributeValue(i));
								if (!containsWantedTag(tags.split(" "))) {
									break switching;
								}
							}
						}
					} else {
						break switching;
					}

					for (int i = 0; i < parser.getAttributeCount(); i++) {
						if (parser.getAttributeLocalName(i).equals("Id")) {
							id = parser.getAttributeValue(i);
						}

						if (parser.getAttributeLocalName(i).equals("Body")) {
							body = applyBodyRegEx(parser.getAttributeValue(i));
						}

						if (parser.getAttributeLocalName(i).equals("Title")) {
							title = applyTitleRegEx(parser.getAttributeValue(i));
						}
					}

					writer.append(id + "\t");
					writer.append(title + "\t");
					writer.append(body + "\t");
					writer.append(tags + "\n");

					writer.toString();
					writer.flush();

					break;

				default:
					break;
				}

				counter += 1;
				if (counter % 100000 == 0) {
					System.out.println(counter + " rows processed.");
				}

				parser.next();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String applyTitleRegEx(String text) {
		String result = text.replaceAll("[\\s]+", " ");
		result = result.replaceAll("^\\s", "");
		result = result.replaceAll("\\s$", "");

		return result;
	}

	public static String applyBodyRegEx(String text) {
		String result = text.replaceAll("&lt;|&gt;|&amp;|&quot;|&apos;|&nbsp;|&#39;", " ");
		result = result.replaceAll("\\r\\n|\\r|\\n", " ");
		result = result.replaceAll("<code>.+?</code>", " ");
		result = result.replaceAll("<[^>]+>", " ");
		result = result.replaceAll("(http|ftp|https|res):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])?", " ");
		result = result.replaceAll("[\\s]+", " ");
		result = result.replaceAll("^\\s", "");
		result = result.replaceAll("\\s$", "");

		return result;
	}

	public String applyTagRegEx(String text) {
		String result = text.replaceAll("><", " ");
		result = result.replaceAll("<|>", "");

		return result;
	}

	public boolean containsWantedTag(String[] tags) {
		boolean tagContained = false;
		Set<String> wantedTags = new HashSet<String>(Arrays.asList("javascript", "java", "c#", "php", "android", "jquery", "python", "html"));

		for (int i = 0; i < tags.length; i++) {
			if (wantedTags.contains(tags[i])) {
				tagContained = true;
			}
		}

		return tagContained;
	}

	public boolean isRightDate(String text) {
		boolean rightDate = false;
		String[] creationDate = text.split("-");
		String year = creationDate[0];

		if (year.equals("2016")) {
			rightDate = true;
		}

		return rightDate;
	}
}
