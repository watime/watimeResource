import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class Main {
	static Connection connection = null;
	static Statement statement;
	static final String magicianDB = "jdbc:sqlite:dec20130811.db3";
	static final String zi2pinDB = "jdbc:sqlite:zi2pin20130808.db3";
	static final String UNRECOGNIZED = "unrecognized";
	static final String MULTI = "multi";
	static final String MANUAL = "manual";

	public static void main(String[] args) throws Exception {
//		extractWikiAbstract();
//		filter("wiki_abstract");
//		categorize("wiki_abstract");
//		writeToDatabase("3_wiki_abstract_multi.txt", "jdbc:sqlite:/Volumes/RamDisk/dec20130810.db3",
//				"0", "wiki_abstract_201308_multi", false);

//		extractWikiArticle();
		filter("wiki_articles_tw");
	}

	//step 1
	static void extractWikiAbstract() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("0_wiki_abstract_zht.txt"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("1_wiki_abstract_extracted.txt"));

		String line = reader.readLine();
		int counter = 0;

		while (line != null && line.length() > 0) {
			int start = line.indexOf("<title>Wikipedia：") + 17;
			if (start >= 17) {
				writer.write(line.substring(start, line.indexOf("</title>", start)));
				writer.newLine();
				if (++counter % 1000 == 0) {
					System.out.println(counter);
					System.gc();
				}
			}
			line = reader.readLine();
		}
		writer.close();
		reader.close();
	}

	static void extractWikiArticle() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("0_wiki_articles.txt"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("1_wiki_articles_tw.txt"));

		String line = reader.readLine();
		String oldLine = "";
		long counter = 0;

		try {
			while (true) {
				if (++counter % 10000 == 0) {
					System.out.println(counter / 10000);
					System.gc();
				}
				if (line.length() < 10) {
					line = reader.readLine();
					continue;
				}
				int start = line.indexOf("zh-tw:") + 6;
				if (start < 0) {
					start = line.indexOf("zh-hant:") + 8;
					if (start < 8) start = 0;
				}
				if (start < 6) {
					line = reader.readLine();
					continue;
				}
				int end = line.indexOf(";", start);
				int end2 = line.indexOf(" ", start);
				if (end2 > 0 && end2 < end) end = end2;
				int end3 = line.indexOf("}", start);
				if (end3 > 0 && end3 < end) end = end3;

				if (end < 0) {
					line = reader.readLine();
					continue;
				}
				line = line.substring(start, end);
				if (!line.contains("'") && !line.contains("[") && line.length() > 1) {
					writer.write(line);
					writer.newLine();
				}
				oldLine = line;
				line = reader.readLine();
			}
		} finally {
			System.out.println(oldLine);
			writer.close();
			reader.close();
		}
	}

	//step 2
	static void filter(String readFrom) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("1_" + readFrom + "_extracted.txt"));
//		BufferedReader reader = new BufferedReader(new FileReader("test.tx"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("2_" + readFrom + "_filtered.txt"));
		BufferedWriter nameWriter = new BufferedWriter(new FileWriter("2_" + readFrom + "_names.txt"));

		String line = reader.readLine();
		int counter = 0;

		while (line != null && line.length() > 0) {
			line = line.trim();
			String[] del = {"'", "(", "（", " ", "_", ",", ".", "?", "!", "+", "@", "#", "$", "%", "^", "&", "*", "=",
					"[", "{", "\\", ":", ";", "，", "。", "？", "！", "-", "、", "：", "；", "－"};
			for (int i = 0; i < 2; i++)
				for (String s : del) {
					int start = line.indexOf(s);
					if (start >= 0) line = line.substring(0, start);
				}

			if (line.length() > 1 && line.length() < 15 && !line.matches(".*\\w.*") && !line.matches(".*[0-9].*") &&
					!line.contains("列表") && !line.contains("年表") && !line.contains("—") &&
					!line.matches(".*[\\u3040-\\u3096].*") && !line.matches(".*[\\u30A0-\\u30FF].*") &&
					!line.contains("中華人民共和國") && !line.contains("共產黨") && !line.contains("歷任") &&
					!line.contains("人民法院") && !line.contains("解放軍") && !line.contains("全國人民代表大會") &&
					!alreadyInDatabase(line)) {
				if (line.contains("·")) {
					nameWriter.write(line);
					nameWriter.newLine();
				} else {
					writer.write(line);
					writer.newLine();
				}
			}

			if (++counter % 1000 == 0) {
				System.out.println(counter);
				System.gc();
			}
			line = reader.readLine();
		}
		reader.close();
		writer.close();
		nameWriter.close();
		connection.close();
		connection = null;
	}

	//step 3
	static void categorize(String readFrom) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("2_" + readFrom + "_filtered.txt"));
//		BufferedReader reader = new BufferedReader(new FileReader("test.tx"));
		BufferedWriter unrecognized = new BufferedWriter(new FileWriter("3_" + readFrom + "_unrecognized.txt"));
		BufferedWriter unique = new BufferedWriter(new FileWriter("3_" + readFrom + "_unique.txt"));
		BufferedWriter multi = new BufferedWriter(new FileWriter("3_" + readFrom + "_multi.txt"));
		BufferedWriter manual = new BufferedWriter(new FileWriter("3_" + readFrom + "_manual.txt"));

		String line = reader.readLine();
		int counter = 0;

		while (line != null && line.length() > 0) {
			String pin = zi2pin(manualReducer(line));
			if (pin.equals(UNRECOGNIZED)) {
				unrecognized.write(line);
				unrecognized.newLine();
			} else if (pin.equals(MULTI)) {
				ArrayList<String> mp = multiPinyin(manualReducer(line));
				if (mp != null) {
					for (String s : multiPinyin(manualReducer(line))) {
						multi.write(line + "@" + s);
						multi.newLine();
					}
				}
			} else if (pin.equals(MANUAL)) {
				manual.write(line);
				manual.newLine();
			} else {
				unique.write(line + "@" + pin);
				unique.newLine();
			}
			if (++counter % 1000 == 0) {
				System.out.println(counter);
				System.gc();
			}
			line = reader.readLine();
		}
		connection.close();
		connection = null;
		reader.close();
		unrecognized.close();
		unique.close();
		multi.close();
		manual.close();
	}

	//step 4
	static void writeToDatabase(String filename, String database, String priority, String comment, boolean replacing) throws Exception {
		if (connection != null) {
			try {
				connection.close();
			} finally {
				connection = null;
			}
		}
		SQLiteConfig config = new SQLiteConfig();
		SQLiteDataSource dataSource = new SQLiteDataSource(config);
		dataSource.setUrl(database);
		connection = dataSource.getConnection();
		statement = connection.createStatement();
		int counter = 0;

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null) {
			if (line.length() < 1) break;
			String[] sp = line.split("@");
			String db;
			if (sp[0].length() == 1) db = "solo";
			else if (sp[0].length() == 2) db = "duet";
			else if (sp[0].length() == 3) db = "trio";
			else if (sp[0].length() == 4) db = "quartet";
			else if (sp[0].length() > 4) db = "concerto";
			else {
				System.out.println("[writeToDB Error]" + line);
				line = reader.readLine();
				continue;
			}
			ResultSet exist = statement.executeQuery("select * from " + db + " where zi glob '" + sp[0] + "';");
			if (!exist.next()) {
				if (replacing) {
					statement.executeUpdate("delete from " + db + " where pinyin glob '" + sp[1] + "' and priority < 1;");
				}
				statement.executeUpdate("insert into " + db + "(pinyin, zi, priority, comment) values('" +
						sp[1] + "', '" + sp[0] + "', " + priority + ", '" + comment + "');");
			}
			exist.close();
			line = reader.readLine();

			if (++counter % 1000 == 0) {
				System.out.println(counter);
				System.gc();
			}
		}

		reader.close();
		connection.close();
		connection = null;
	}

	static boolean alreadyInDatabase(String s) throws Exception {
		if (connection == null) {
			SQLiteConfig config = new SQLiteConfig();
			SQLiteDataSource dataSource = new SQLiteDataSource(config);
			dataSource.setUrl(magicianDB);
			connection = dataSource.getConnection();
			statement = connection.createStatement();
		}
		ResultSet rs;
		try {
			switch (s.length()) {
				case 1:
					throw new Exception("string length = 1");
				case 2:
					rs = statement.executeQuery("select * from duet where zi glob '" + s + "';");
					break;
				case 3:
					rs = statement.executeQuery("select * from trio where zi glob '" + s + "';");
					break;
				case 4:
					rs = statement.executeQuery("select * from quartet where zi glob '" + s + "';");
					break;
				default:
					rs = statement.executeQuery("select * from concerto where zi glob '" + s + "';");
					break;
			}
			if (rs.next()) {
				rs.close();
				return true;
			}
			rs.close();
			return false;
		} catch (Exception e) {
			System.out.println(s);
			e.printStackTrace();
			return true;
		}
	}

	static String zi2pin(String zi) throws Exception {
		StringBuilder pin = new StringBuilder("");
		if (connection == null) {
			SQLiteConfig config = new SQLiteConfig();
			SQLiteDataSource dataSource = new SQLiteDataSource(config);
			dataSource.setUrl(zi2pinDB);
			connection = dataSource.getConnection();
			statement = connection.createStatement();
		}
		for (int i = 0; i < zi.length(); i++) {
			ResultSet rs = statement.executeQuery("select * from z where zi like '" +
					zi.substring(i, i + 1) + "';");
			if (!rs.next()) {
				rs.close();
				return UNRECOGNIZED;
			} else if (rs.getInt(2) > 1) {
				rs.close();
				return MULTI;
			} else if (rs.getInt(2) < -1) {
				rs.close();
				return MANUAL;
			} else if (rs.getInt(2) == 1) {
				pin.append(rs.getString(3));
			} else throw new Exception("unexpected count value in zi2pin database");
		}
		return pin.toString();
	}

	static ArrayList<String> multiPinyin(String original) throws Exception {
		if (connection == null) {
			SQLiteConfig config = new SQLiteConfig();
			SQLiteDataSource dataSource = new SQLiteDataSource(config);
			dataSource.setUrl(zi2pinDB);
			connection = dataSource.getConnection();
			statement = connection.createStatement();
		}
		ArrayList<String> ziWithMultiPinyin = new ArrayList<String>();
		ArrayList<String[]> pinyins = new ArrayList<String[]>();
		ArrayList<String> output = new ArrayList<String>();

		StringBuilder o2 = new StringBuilder();
		for (int i = 0; i < original.length(); i++) {
			String zi = original.substring(i, i + 1);
			ResultSet rs = statement.executeQuery("select * from z where zi like '" + zi + "';");
			if (!rs.next()) {
				multiPinyinErrorLog(original + "@" + zi + "[233]");
				return null;
			}
			int count = rs.getInt(2);
			if (count == 1) o2.append(rs.getString(3));
			else {
				o2.append(zi);
				if (!ziWithMultiPinyin.contains(zi)) {
					ziWithMultiPinyin.add(zi);
					if (count == 2) pinyins.add(new String[]{rs.getString(3), rs.getString(4)});
					else if (count == 3) pinyins.add(new String[]{rs.getString(3), rs.getString(4), rs.getString(5)});
					else if (count == 4)
						pinyins.add(new String[]{rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)});
					else {
						multiPinyinErrorLog(original + "@" + zi + "[247]");
						return null;
					}
				}
			}
		}
		for (String s1 : pinyins.get(0)) {
			String a1 = o2.toString().replaceAll(ziWithMultiPinyin.get(0), s1);
			if (ziWithMultiPinyin.size() == 1)
				output.add(a1);
			else
				for (String s2 : pinyins.get(1)) {
					String a2 = a1.replaceAll(ziWithMultiPinyin.get(1), s2);
					if (ziWithMultiPinyin.size() == 2)
						output.add(a2);
					else
						for (String s3 : pinyins.get(2)) {
							String a3 = a2.replaceAll(ziWithMultiPinyin.get(2), s3);
							if (ziWithMultiPinyin.size() == 3)
								output.add(a3);
							else multiPinyinErrorLog(original + "[OVERFLOW]");
						}
				}
		}
		return output;
	}

	static void multiPinyinErrorLog(String s) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter("4_multi_cannot_process.txt", true));
		writer.write(s);
		writer.newLine();
		writer.close();
	}

	static String manualReducer(String s) {
		return s.replaceAll("基金會", "基金回")
				.replaceAll("進行", "進星")
				.replaceAll("行政", "星政")
				.replaceAll("遊行", "遊星")
				.replaceAll("傳輸", "船輸")
				.replaceAll("社會", "社灰")
				.replaceAll("長江", "常江")
				.replaceAll("行動", "星動")
				.replaceAll("運行", "運星")
				.replaceAll("共和國", "共河國")
				.replaceAll("委員會", "委員灰")
				.replaceAll("議會", "議灰")
				.replaceAll("協會", "協灰")
				.replaceAll("銀行", "銀航")
				.replaceAll("調查", "掉查")
				.replaceAll("學會", "學灰")
				.replaceAll("音樂", "音月")
				.replaceAll("教會", "教灰")
				.replaceAll("分校", "分笑")
				.replaceAll("學校", "學笑")
				.replaceAll("遺傳", "遺船")
				.replaceAll("長崎", "常崎")
				.replaceAll("行為", "星為")
				.replaceAll("大會", "大灰")
				.replaceAll("運動會", "運動灰")
				.replaceAll("重慶", "蟲慶")
				.replaceAll("可樂", "可勒")
				.replaceAll("長榮", "常榮")
				.replaceAll("拓跋", "拖跋")
				.replaceAll("視覺", "視決")
				.replaceAll("傳播", "船播")
				.replaceAll("會議", "灰議")
				.replaceAll("子彈", "子蛋")
				.replaceAll("機會", "機灰")
				.replaceAll("流行", "流星")
				.replaceAll("長鰭", "常鰭")
				.replaceAll("長身", "常身")
				.replaceAll("長吻", "常吻")
				.replaceAll("長腳", "常腳")
				.replaceAll("長葉", "常葉")
				.replaceAll("首都", "首肚")
				.replaceAll("長安", "常安")
				.replaceAll("彈性", "彈星")
				.replaceAll("運行", "運星")
				.replaceAll("平行", "平星")
				.replaceAll("俱樂部", "俱勒部")
				.replaceAll("大咽", "大眼")
				.replaceAll("傳媒", "船媒")
				.replaceAll("參數", "殘數")
				.replaceAll("的", "德")
				.replaceAll("研討會", "研討灰")
				.replaceAll("研究會", "研究灰");
	}
}
