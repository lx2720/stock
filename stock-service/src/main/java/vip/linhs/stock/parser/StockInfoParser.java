package vip.linhs.stock.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import vip.linhs.stock.exception.ServiceException;
import vip.linhs.stock.model.po.StockInfo;
import vip.linhs.stock.util.StockConsts.StockState;
import vip.linhs.stock.util.StockUtil;

@Component
public class StockInfoParser {

    public List<StockInfo> parseStockInfoList(String content) {
        content = content.replaceAll("Ａ", "A");
        Pattern pattern = Pattern.compile(
                "<a href=\"http://finance.ifeng.com/app/hq/stock/(sh|sz)\\S{1,20}/index\\.shtml\" target=\"_blank\">(\\S{1,20}?)\\((\\S{1,8})\\)</a>");
        Matcher matcher = pattern.matcher(content);

        ArrayList<StockInfo> list = new ArrayList<>();

        while (matcher.find()) {
            String exchange = matcher.group(1);
            String name = matcher.group(2);
            String code = matcher.group(3);

            if (!StockUtil.isStockCode(code)) {
                continue;
            }

            StockInfo stockInfo = new StockInfo();
            stockInfo.setExchange(exchange);
            stockInfo.setName(name);
            stockInfo.setCode(code);

            list.add(stockInfo);
        }
        return list;
    }

    public StockState parseStockState(String content) {
        Assert.notNull("'content' must not be null", content);
        throw new ServiceException("Not yet implemented");
    }

}
