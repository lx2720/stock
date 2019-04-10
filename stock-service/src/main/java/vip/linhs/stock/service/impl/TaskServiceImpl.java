package vip.linhs.stock.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import vip.linhs.stock.dao.ExecuteInfoDao;
import vip.linhs.stock.dao.RobotDao;
import vip.linhs.stock.dao.TickerConfigDao;
import vip.linhs.stock.exception.ServiceException;
import vip.linhs.stock.model.po.DailyIndex;
import vip.linhs.stock.model.po.ExecuteInfo;
import vip.linhs.stock.model.po.Message;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.model.po.StockLog;
import vip.linhs.stock.model.po.Task;
import vip.linhs.stock.service.HolidayCalendarService;
import vip.linhs.stock.service.MessageService;
import vip.linhs.stock.service.StockCrawlerService;
import vip.linhs.stock.service.StockService;
import vip.linhs.stock.service.TaskService;
import vip.linhs.stock.util.DecimalUtil;
import vip.linhs.stock.util.StockConsts;
import vip.linhs.stock.util.StockConsts.TaskState;
import vip.linhs.stock.util.StockUtil;

@Service
public class TaskServiceImpl implements TaskService {

    private final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private Map<String, BigDecimal> tickerMap = new HashMap<>();

    @Autowired
    private HolidayCalendarService holidayCalendarService;

    @Autowired
    private ExecuteInfoDao executeInfoDao;

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Autowired
    private StockService stockService;

    @Autowired
    private TickerConfigDao tickerConfigDao;

    @Autowired
    private RobotDao robotDao;

    @Autowired
    private MessageService messageServicve;

    @Override
    public List<ExecuteInfo> getPendingTaskListById(int... id) {
        return executeInfoDao.getByTaskIdAndState(id, TaskState.Pending.value());
    }

    @Override
    public void executeTask(ExecuteInfo executeInfo) {
        executeInfo.setStartTime(new Date());
        executeInfo.setMessage("");
        int id = executeInfo.getTaskId();
        Task task = Task.valueOf(id);
        try {
            switch (task) {
            case BeginOfYear:
                holidayCalendarService.updateCurrentYear();
                break;
            case EndOfYear:
                break;
            case BeginOfDay:
                tickerMap.clear();
                break;
            case EndOfDay:
                break;
            case UpdateOfStock:
                runUpdateOfStock();
                break;
            case UpdateOfStockState:
                runUpdateOfStockState();
                break;
            case UpdateOfDailyIndex:
                runUpdateOfDailyIndex();
                break;
            case Ticker:
                runTicker();
                break;
            default:
                break;
            }
        } catch (Exception e) {
            executeInfo.setMessage(e.getMessage());
            logger.error(e.getMessage(), e);

            List<Map<String, Object>> robotList = robotDao
                    .getByType(StockConsts.RobotType.DingDing.value());
            if (!robotList.isEmpty()) {
                Map<String, Object> robot = robotList.get(0);
                String target = (String) robot.get("webhook");
                Message message = new Message(StockConsts.MessageType.DingDing.value(), target,
                        String.format("task: %s, error: %s", task.getName(), e.getMessage()),
                        new Date());
                messageServicve.sendDingding(message);
            }
        }

        executeInfo.setCompleteTime(new Date());
        executeInfoDao.update(executeInfo);
    }

    private void runUpdateOfStock() {
        List<StockInfo> list = stockService.getAll().stream().filter(stockInfo ->
               !StockUtil.isCompositeIndex(stockInfo.getExchange(), stockInfo.getCode())
        ).collect(Collectors.toList());
        Map<String, List<StockInfo>> stockMap = list.stream().collect(Collectors.groupingBy(StockInfo::getCode));

        ArrayList<StockInfo> needAddedList = new ArrayList<>();
        ArrayList<StockInfo> needUpdatedList = new ArrayList<>();
        ArrayList<StockLog> stockLogList = new ArrayList<>();

        final Date date = new Date();

        List<StockInfo> crawlerList = stockCrawlerService.getStockList();
        for (StockInfo stockInfo : crawlerList) {
            StockConsts.StockLogType stocLogType = null;
            List<StockInfo> stockGroupList = stockMap.get(stockInfo.getCode());
            stockInfo.setAbbreviation(TaskServiceImpl.getPinyin(stockInfo.getName()));
            String oldValue = null;
            String newValue = null;
            if (stockGroupList == null) {
                stocLogType = StockConsts.StockLogType.New;
                oldValue = "";
                newValue = stockInfo.getName();
            } else {
                StockInfo stockInfoInDb = stockGroupList.get(0);
                if (!stockInfo.getName().equals(stockInfoInDb.getName())) {
                    stocLogType = StockConsts.StockLogType.Rename;
                    oldValue = stockInfoInDb.getName();
                    newValue = stockInfo.getName();
                    stockInfo.setId(stockInfoInDb.getId());
                }
            }

            if (stocLogType != null) {
                StockLog stockLog = new StockLog(stockInfo.getId(), date, stocLogType.value(),
                        oldValue, newValue);
                if (stocLogType == StockConsts.StockLogType.New) {
                    needAddedList.add(stockInfo);
                } else {
                    needUpdatedList.add(stockInfo);
                }
                stockLogList.add(stockLog);
            }
        }

        stockService.update(needAddedList, needUpdatedList, stockLogList);
    }

    private void runUpdateOfStockState() {
        List<StockInfo> list = stockService.getAllListed();

        for (StockInfo stockInfo : list) {
            StockConsts.StockState state = stockCrawlerService.getStockState(stockInfo.getCode());
            if (stockInfo.getState() != state.value()) {
                StockConsts.StockLogType stockLogType = null;
                switch (state) {
                case Listed:
                    stockLogType = StockConsts.StockLogType.ReListed;
                    break;
                case Terminated:
                    stockLogType = StockConsts.StockLogType.Terminated;
                    break;
                case Delisted:
                    stockLogType = StockConsts.StockLogType.Delisted;
                    break;
                default:
                    throw new ServiceException("未找到状态" + state);
                }
                StockLog stockLog = new StockLog(stockInfo.getId(), new Date(), stockLogType.value(),
                        String.valueOf(stockInfo.getState()), String.valueOf(state.value()));
                stockInfo.setState(state.value());
                stockService.update(null, Arrays.asList(stockInfo), Arrays.asList(stockLog));
            }
        }
    }

    private void runUpdateOfDailyIndex() {
        if (stockService.existsTodayDailyIndex()) {
            return;
        }

        List<StockInfo> list = stockService.getAll().stream()
                .filter(stockInfo -> stockInfo.getState() != StockConsts.StockState.Delisted.value()
                        && stockInfo.getState() != StockConsts.StockState.Terminated.value())
                .collect(Collectors.toList());
        final String currentDateStr = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        ArrayList<DailyIndex> needSaveList = new ArrayList<>();
        for (StockInfo stockInfo : list) {
            DailyIndex dailyIndex = stockCrawlerService.getDailyIndex(stockInfo.getExchange() + stockInfo.getCode());
            if (dailyIndex != null
                    && DecimalUtil.bg(dailyIndex.getOpeningPrice(), BigDecimal.ZERO)
                    && dailyIndex.getTradingVolume() > 0
                    && DecimalUtil.bg(dailyIndex.getTradingValue(), BigDecimal.ZERO)
                    && currentDateStr.equals(DateUtils.formatDate(dailyIndex.getDate(), "yyyy-MM-dd"))) {
                dailyIndex.setStockInfoId(stockInfo.getId());
                needSaveList.add(dailyIndex);
            }
        }
        if (!needSaveList.isEmpty()) {
            stockService.saveDailyIndex(needSaveList);
        }
    }

    private void runTicker() {
        List<Map<String, Object>> tickerConfigList = tickerConfigDao
                .getValuesByKey(StockConsts.TickerConfigKey.StockList.value());
        if (!tickerConfigList.isEmpty()) {
            Map<String, Object> tickerConfig = tickerConfigList.get(0);
            List<String> stockCodeList = Arrays.asList(((String) tickerConfig.get("value")).split(","));
            String target = (String) tickerConfig.get("webhook");
            stockCodeList.forEach(code -> {
                DailyIndex dailyIndex = stockCrawlerService.getDailyIndex(code);
                if (tickerMap.containsKey(code)) {
                    BigDecimal lastPrice = tickerMap.get(code);
                    double rate = Math
                            .abs(StockUtil.calcIncreaseRate(dailyIndex.getClosingPrice(), lastPrice)
                                    .movePointRight(2).doubleValue());
                    if (Double.compare(rate, 2) >= 0) {
                        tickerMap.put(code, dailyIndex.getClosingPrice());
                        String content = String.format("%s:当前价格:%.02f, 涨幅%.02f%%", code,
                            dailyIndex.getClosingPrice().doubleValue(),
                            StockUtil.calcIncreaseRate(dailyIndex.getClosingPrice(),
                                    dailyIndex.getPreClosingPrice()).movePointRight(2).doubleValue());
                        Message message = new Message(StockConsts.MessageType.DingDing.value(),
                                target, content, new Date());
                        messageServicve.sendDingding(message);
                    }
                } else {
                    tickerMap.put(code, dailyIndex.getPreClosingPrice());
                    String content = String.format("%s:当前价格:%.02f", code,
                            dailyIndex.getClosingPrice().doubleValue());
                    Message message = new Message(StockConsts.MessageType.DingDing.value(), target,
                            content, new Date());
                    messageServicve.sendDingding(message);
                }
            });
        }
    }

    private static String getPinyin(String name) {
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        StringBuilder sb = new StringBuilder();
        for (char ch : name.toLowerCase().toCharArray()) {
            if (ch == '*') {
                continue;
            }
            if (ch >= 'a' && ch <= 'z') {
                sb.append(ch);
            } else if (ch == '行') {
                sb.append('h');
            } else {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat);
                    if (arr == null) {
                        throw new ServiceException("not support character " + name);
                    }
                    sb.append(arr[0].charAt(0));
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    throw new ServiceException("get pinyin error", e);
                }
            }
        }
        return sb.toString();
    }

}
