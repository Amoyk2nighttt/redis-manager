package com.newegg.ec.redis.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.newegg.ec.redis.entity.*;
import com.newegg.ec.redis.service.IClusterService;
import com.newegg.ec.redis.service.INodeInfoService;
import com.newegg.ec.redis.service.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Jay.H.Zou
 * @date 7/20/2019
 */
@RequestMapping("/monitor/*")
@Controller
public class MonitorController {

    @Autowired
    private INodeInfoService nodeInfoService;

    @Autowired
    private IRedisService redisService;

    @Autowired
    private IClusterService clusterService;

    @RequestMapping(value = "/getInfoItemMonitorData", method = RequestMethod.POST)
    @ResponseBody
    public Result getInfoItemMonitorData(@RequestBody NodeInfoParam nodeInfoParam) {
        Integer clusterId = nodeInfoParam.getClusterId();
        if (clusterId == null) {
            return Result.failResult();
        }
        Cluster cluster = clusterService.getClusterById(clusterId);
        if (cluster == null) {
            return Result.failResult();
        }
        List<NodeInfo> nodeInfoList = nodeInfoService.getNodeInfoList(nodeInfoParam);
        if (nodeInfoList == null) {
            return Result.failResult();
        }
        if (nodeInfoList.isEmpty()) {
            return Result.successResult(nodeInfoList);
        }
        Multimap<String, JSONObject> nodeInfoListMap = ArrayListMultimap.create();

        nodeInfoList.forEach(nodeInfo -> nodeInfoListMap.put(nodeInfo.getNode(), compressionNodeInfo(nodeInfoParam.getInfoItem(), nodeInfo)));
        List<Collection<JSONObject>> nodeInfoDataList = new ArrayList<>();
        nodeInfoListMap.keySet().forEach(key -> {
            Collection<JSONObject> oneNodeInfoList = nodeInfoListMap.get(key);
            nodeInfoDataList.add(oneNodeInfoList);
        });
        return Result.successResult(nodeInfoDataList);
    }

    private JSONObject compressionNodeInfo(String infoItem, NodeInfo nodeInfo) {
        String nodeInfoField = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, infoItem);
        JSONObject original = JSONObject.parseObject(JSONObject.toJSONString(nodeInfo));
        JSONObject info = new JSONObject();
        info.put("role", nodeInfo.getRole());
        info.put("node", nodeInfo.getNode());
        info.put(nodeInfoField, original.getDoubleValue(nodeInfoField));
        info.put("updateTime", nodeInfo.getUpdateTime());
        return info;
    }


    @RequestMapping(value = "/getSlowLogList", method = RequestMethod.POST)
    @ResponseBody
    public Result getSlowLogList(@RequestBody SlowLogParam slowLogParam) {
        Cluster cluster = clusterService.getClusterById(slowLogParam.getClusterId());
        if (cluster == null) {
            return Result.failResult();
        }
        List<RedisSlowLog> redisSlowLog = redisService.getRedisSlowLog(cluster, slowLogParam);
        return Result.successResult(redisSlowLog);
    }

}
