import com.kosa.showfan.artist.dao.ArtistDAO;
import com.kosa.showfan.artist.dto.ArtistDTO;
import com.kosa.showfan.aws.AWSService;
import com.kosa.showfan.cast.dao.CastDAO;
import com.kosa.showfan.cast.dto.CastDTO;
import com.kosa.showfan.mybatis.MyBatisConnectionFactory;
import com.kosa.showfan.seat.dao.SeatDAO;
import com.kosa.showfan.seat.dto.SeatDTO;
import com.kosa.showfan.show.dao.ShowDAO;
import com.kosa.showfan.show.dto.ShowDTO;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

public class Main {
    private static final String url = "http://www.kopis.or.kr/openApi/restful";
    // need data
    private static final String key = "";

    public static void main(String[] args) {
        // 테스트
        // 축제 목록 처리
//        String requestAPI = String.format("%s/prffest?service=%s&stdate=20230801&eddate=20230831&rows=10&cpage=4", url, key);
//        insertDataToDB(requestAPI, true);

        // 나머지 공연 처리
//        String requestAPI = String.format("%s/pblprfr?service=%s&stdate=20230801&eddate=20230831&rows=10&cpage=1", url, key);
//        insertDataToDB(requestAPI, false);

        // 반복
        // 총 검색 건수 : 6,879
//        for (int i = 1; i <= 1; i++) {
//            String startdate = "20140101";
//            String enddate = "20231212";
//            String requestAPILoop = String.format("%s/prffest?service=%s&stdate=%s&eddate=%s&rows=6879&cpage=%d", url, key, startdate, enddate, i);
//            insertDataToDB(requestAPILoop, true);
//        }
        // 총 검색 건수 : 84,987
        for (int i = 1; i <= 8; i++) {
            String startdate = "20140101";
            String enddate = "20231212";
            String requestAPILoop = String.format("%s/pblprfr?service=%s&stdate=%s&eddate=%s&rows=10000&cpage=%d", url, key, startdate, enddate, i);
            insertDataToDB(requestAPILoop, false);
        }
    }

    private static void insertDataToDB(String requestAPI, boolean isFestival) {
        try {
            NodeList festivalListNode = getHttpData(requestAPI);
            System.out.println("data call success");
            // 공연 목록 파싱
            for (int i = 0; i < festivalListNode.getLength(); i++) {
                ShowDAO showDAO = new ShowDAO(MyBatisConnectionFactory.getSqlSessionFactory());
                ShowDTO showDTO = new ShowDTO();

                Node node = festivalListNode.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String showId = getValue("mt20id", element);
                    // 현재 DB에 없는 경우 DB에 데이터 삽입
                    if (showDAO.selectById(showId) == null) {
//                    if (true) {
                        showDTO.setShowId(showId);
                        if (isFestival) {
                            showDTO.setGenreId(5L);
                        } else {
                            String genre = getValue("genrenm", element);
                            if (genre.equals("연극")) {
                                showDTO.setGenreId(1L);
                            } else if (genre.equals("서양음악(클래식)")) {
                                showDTO.setGenreId(3L);
                            } else if (genre.equals("대중음악")) {
                                showDTO.setGenreId(4L);
                            } else if (genre.equals("뮤지컬")) {
                                showDTO.setGenreId(2L);
                            }
                        }
                        showDTO.setShowName(checkNull(getValue("prfnm", element)));

                        // 축제 공연 상세 조회
                        requestAPI = String.format("%s/pblprfr/%s?service=%s", url, showId, key);
                        NodeList festivalInfoNode = getHttpData(requestAPI);
                        for (int j = 0; j < festivalInfoNode.getLength(); j++) {
                            node = festivalInfoNode.item(j);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                element = (Element) node;

                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
                                if (checkNull(getValue("prfpdfrom", element)) != null) {
                                    Date startDate = formatter.parse(getValue("prfpdfrom", element));
                                    showDTO.setShowStartDay(new java.sql.Date(startDate.getTime()));
                                }
                                if (checkNull(getValue("prfpdto", element)) != null) {
                                    Date endDate = formatter.parse(getValue("prfpdto", element));
                                    showDTO.setShowEndDay(new java.sql.Date(endDate.getTime()));
                                }
                                showDTO.setShowTime(checkNull(getValue("dtguidance", element)));

                                String posterLink = checkNull(getValue("poster", element));
                                AWSService s3 = new AWSService();
                                s3.uploadFile(showId, posterLink);
                                showDTO.setShowPoster(String.format("https://showfan.s3.ap-northeast-2.amazonaws.com/%s.jpg", showId));

                                String showAge = checkNull(getValue("prfage", element));
                                if (showAge != null) {
                                    String intStr = showAge.replaceAll("[^0-9]", "");
                                    int showAgeNum = 0;
                                    if (!intStr.equals("")) {
                                        showAgeNum = Integer.parseInt(intStr);
                                    }
                                    showDTO.setShowAge(showAgeNum);
                                }

                                showDTO.setShowTime(checkNull(getValue("dtguidance", element)));
                                showDTO.setShowTicketingSite(null);
                                showDTO.setShowStory(checkNull(getValue("sty", element)));
                                showDTO.setShowStatus(checkNull(getValue("prfstate", element)));
                                showDTO.setShowRuntime(checkNull(getValue("prfruntime", element)));

                                if (element.getElementsByTagName("styurls").getLength() > 0) {
                                    NodeList imageListNode = element.getElementsByTagName("styurls").item(0).getChildNodes();
                                    for (int l = 0; l < imageListNode.getLength(); l++) {
                                        if (l == 1) {
                                            String image = imageListNode.item(l).getChildNodes().item(0).getNodeValue();
                                            if (image != null) {
                                                AWSService subS3 = new AWSService();
                                                subS3.uploadFile(showId + "_1", image);
                                                showDTO.setShowImage1(String.format("https://showfan.s3.ap-northeast-2.amazonaws.com/%s_1.jpg", showId));
                                            }
                                        } else if (l == 3) {
                                            String image = imageListNode.item(l).getChildNodes().item(0).getNodeValue();
                                            if (image != null) {
                                                AWSService subS3 = new AWSService();
                                                subS3.uploadFile(showId + "_2", image);
                                                showDTO.setShowImage2(String.format("https://showfan.s3.ap-northeast-2.amazonaws.com/%s_2.jpg", showId));
                                            }
                                        } else if (l == 5) {
                                            String image = imageListNode.item(l).getChildNodes().item(0).getNodeValue();
                                            if (image != null) {
                                                AWSService subS3 = new AWSService();
                                                subS3.uploadFile(showId + "_3", image);
                                                showDTO.setShowImage3(String.format("https://showfan.s3.ap-northeast-2.amazonaws.com/%s_3.jpg", showId));
                                            }
                                        } else if (l == 7) {
                                            String image = imageListNode.item(l).getChildNodes().item(0).getNodeValue();
                                            if (image != null) {
                                                AWSService subS3 = new AWSService();
                                                subS3.uploadFile(showId + "_4", image);
                                                showDTO.setShowImage4(String.format("https://showfan.s3.ap-northeast-2.amazonaws.com/%s_4.jpg", showId));
                                            }
                                        }
                                    }
                                }

                                // 공연장 상세 조회
                                String venueId = getValue("mt10id", element);
                                requestAPI = String.format("%s/prfplc/%s?service=%s", url, venueId, key);
                                NodeList DetailVenueNode = getHttpData(requestAPI);
                                for (int k = 0; k < DetailVenueNode.getLength(); k++) {
                                    node = DetailVenueNode.item(k);
                                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                                        Element venueElement = (Element) node;
                                        showDTO.setShowAddress(checkNull(getValue("adres", venueElement)));
                                        showDTO.setShowVenues(checkNull(getValue("fcltynm", venueElement)));
                                        if (checkNull(getValue("la", venueElement)) != null) {
                                            showDTO.setShowLatitude(Double.parseDouble(getValue("la", venueElement)));
                                        }
                                        if (checkNull(getValue("lo", venueElement)) != null) {
                                            showDTO.setShowLongitude(Double.parseDouble(getValue("lo", venueElement)));
                                        }
                                    }
                                }
                            }
                        }
                        showDAO.insert(showDTO);

                        // cast, artist 데이터 삽입
                        String artistName = checkNull(getValue("prfcast", element));
                        if (artistName != null) {
                            ArtistDAO artistDAO = new ArtistDAO(MyBatisConnectionFactory.getSqlSessionFactory());
                            ArtistDTO artistDTO = new ArtistDTO();
                            CastDAO castDAO = new CastDAO(MyBatisConnectionFactory.getSqlSessionFactory());
                            CastDTO castDTO = new CastDTO();
                            if (artistName.trim().substring(artistName.length() - 2).equals(" 등")) {
                                artistName = artistName.substring(0, artistName.length() - 2);
                            }
                            String[] artists = artistName.split(", ");

                            boolean isCastExist = true;
                            if (castDAO.selectByShowId(showId).size() == 0) {
                                isCastExist = false;
                            }
                            HashSet<String> hashSet = new HashSet<>(Arrays.asList(artists));
                            artists = hashSet.toArray(new String[0]);
                            for (String artist : artists) {
                                if (artistDAO.selectByName(artist) == null) {
                                    artistDTO.setArtistName(artist);
                                    artistDTO.setArtistImage(null);
                                    artistDAO.insert(artistDTO);
                                }
                                if (!isCastExist) {
                                    castDTO.setShowId(showId);
                                    long artistId = artistDAO.selectByName(artist).getArtistId();
                                    castDTO.setArtistId(artistId);
                                    castDAO.insert(castDTO);
                                }
                            }
                        }

                        // 공연 가격 데이터 삽입
                        String ticketPrice = checkNull(getValue("pcseguidance", element));
                        if (ticketPrice != null) {
                            SeatDAO seatDAO = new SeatDAO(MyBatisConnectionFactory.getSqlSessionFactory());
                            SeatDTO seatDTO = new SeatDTO();
                            String[] seatPrice = ticketPrice.trim().split(", ");
                            boolean isSeat = true;
                            if (seatDAO.selectByShowId(showId).size() == 0) {
                                isSeat = false;
                            }
                            if (!isSeat) {
                                for (String price : seatPrice) {
                                    String seatName = "";
                                    int priceLength = price.split(" ").length;
                                    for (int p = 0; p < priceLength - 1; p++) {
                                        seatName += price.split(" ")[p];
                                    }
                                    seatDTO.setSeatName(seatName);
                                    int priceNumber = 0;
                                    if (priceLength != 1) {
                                        priceNumber = Integer.parseInt(price.split(" ")[priceLength - 1].replaceAll("[^0-9]", ""));
                                    }
                                    seatDTO.setSeatPrice(priceNumber);
                                    seatDTO.setShowId(showId);
                                    seatDAO.insert(seatDTO);
                                }
                            }
                        }
                    }
                    System.out.println(showDAO.selectById(showId));
                    System.out.println("----------------------" + i + " " + festivalListNode.getLength());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static NodeList getHttpData(String requestAPI) throws IOException, ParserConfigurationException, SAXException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(requestAPI);
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
        // check api status
//        System.out.println(httpResponse.getStatusLine());
        BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
        String inputLine;
        StringBuffer res = new StringBuffer();
        while ((inputLine = br.readLine()) != null) {
            res.append(inputLine);
        }
        br.close();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;

        InputSource is = new InputSource(new StringReader(res.toString()));
        builder = factory.newDocumentBuilder();
        doc = builder.parse(is);

        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        httpClient.close();
        return children;
    }

    private static String getValue(String tag, Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    private static String checkNull(String str) {
        return str.equals(" ") ? null : str;
    }
}