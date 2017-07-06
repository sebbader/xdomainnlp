package xdomainnlp.noveltydetection.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostsTopicsMerger {
	private String postsInputFile = "C:\\so-index\\posts_short.txt";
	private String topicsInputFile = "C:\\so-index\\posts_doc-topics.txt";
	private String outputFile = "C:\\so-index\\posts_index-topics_merged.txt";

	public void run() throws FileNotFoundException, IOException {
		try {
			FileInputStream ifis = new FileInputStream(postsInputFile);
			FileInputStream tfis = new FileInputStream(topicsInputFile);

			BufferedReader postsReader = new BufferedReader(new InputStreamReader(ifis, "utf-8"));
			BufferedReader topicsReader = new BufferedReader(new InputStreamReader(tfis, "utf-8"));
			BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));

			String postsLine = "";
			String topicsLine = "";

			int counter = 0;

			outer: while ((postsLine = postsReader.readLine()) != null) {
				String[] postsLineParts = postsLine.split("\t");

				topicsReader.mark(50000);

				while ((topicsLine = topicsReader.readLine()) != null) {
					String[] topicsLineParts = topicsLine.split("\t");

					if (postsLineParts[0].equals(topicsLineParts[1])) {
						String result = merge(postsLineParts, topicsLineParts);

						if (result != null) {
							outputWriter.write(result + "\n");
							outputWriter.flush();

							++counter;

							if (counter % 10000 == 0) {
								System.out.println(counter + " lines processed.");
							}
						}
						break;
					} else {
						topicsReader.reset();
						continue outer;
					}
				}
			}

			outputWriter.close();
			postsReader.close();
			topicsReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String merge(String[] postsLineParts, String[] topicsLineParts) {
		List<String> postsLines = new ArrayList<>(Arrays.asList(postsLineParts));
		List<String> topicsLines = new ArrayList<>(Arrays.asList(topicsLineParts));

		topicsLines.remove(0);
		topicsLines.remove(0);

		String topicsLine = String.join(" ", topicsLines);

		postsLines.add(topicsLine);

		String result = String.join("\t", postsLines);

		return result;
	}
}
