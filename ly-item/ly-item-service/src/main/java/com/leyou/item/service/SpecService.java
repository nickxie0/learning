package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpecParamDTO;
import com.leyou.item.entity.SpecGroup;
import com.leyou.item.entity.SpecParam;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpecService {
    @Autowired
    private SpecGroupMapper groupMapper;

    @Autowired
    private SpecParamMapper paramMapper;

    /**
     * 根据分类id查询规格组
     * @param cid
     * @return
     */
    public List<SpecGroupDTO> querySpecGroupByCid(Long cid) {
        //根据分类查询规格组
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> list = groupMapper.select(specGroup);

        //健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        //将SpecGroup类型转换成SpecGroupDTO类型并返回
        return BeanHelper.copyWithCollection(list, SpecGroupDTO.class);
    }

    /**
     * 根据规格组id查询规格参数
     *
     * @param gid
     * @return
     */
    public List<SpecParamDTO> queryParamByGid(Long gid) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        List<SpecParam> list = paramMapper.select(specParam);
        //健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SpecParamDTO.class);

    }


    /**
     * 根据条件查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    public List<SpecParamDTO> querySpecParams(Long gid, Long cid, Boolean searching) {
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        List<SpecParam> list = paramMapper.select(param);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SpecParamDTO.class);
    }

    /**
     * 根据分类id查询规格组及组内参数
     * @param id
     * @return
     */
    public List<SpecGroupDTO> querySpecs(Long id) {
        List<SpecGroupDTO> groupList = querySpecGroupByCid(id);

        List<SpecParamDTO> params = querySpecParams(null, id, null);

        //尝试对params进行分组，根据groupId，结果应该是Map<Long,List<SpecParamDTO>>
        Map<Long, List<SpecParamDTO>> map = params.stream()
                .collect(Collectors.groupingBy(SpecParamDTO::getGroupId));

       /* Map<Long, List<SpecParamDTO>> map = new HashMap<>();

        for (SpecParamDTO param : params) {
            if (!map.containsKey(param.getGroupId())) {
                //当前不存在该组，则创建组
                map.put(param.getGroupId(), new ArrayList<>());
            }
            //存在该组，则进入该组
            map.get(param.getGroupId()).add(param);
        }*/

        for (SpecGroupDTO group : groupList) {
            group.setParams(map.get(group.getId()));
        }
        return groupList;
    }
}
