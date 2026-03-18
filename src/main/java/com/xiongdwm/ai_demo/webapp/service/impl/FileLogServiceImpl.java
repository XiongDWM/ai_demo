package com.xiongdwm.ai_demo.webapp.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.xiongdwm.ai_demo.webapp.entities.AiSysUser;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.repository.AiSysUserRepository;
import com.xiongdwm.ai_demo.webapp.repository.FileLogRepo;
import com.xiongdwm.ai_demo.webapp.repository.KnowledgeBaseRepository;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
public class FileLogServiceImpl implements FileLogService{
    
    @Resource
    private FileLogRepo fileLogRepo;
    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private AiSysUserRepository aiSysUserRepository;

    private final Sinks.Many<List<KnowledgeBase>> sink = Sinks.many().replay().latest();

    @Override
    public List<FileLog> showFileLog(Long knowledgeBaseId) {
        if(knowledgeBaseId!=null&&knowledgeBaseId>0L){
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
            if(knowledgeBase ==null)return Collections.emptyList();

            return knowledgeBase.getFileLogs(); // Return an empty list if the knowledge base is not found
        }
        return fileLogRepo.findAll();
    }

    @Override
    public void saveFileLog(FileLog fileLog) {
        fileLogRepo.saveAndFlush(fileLog);
    }

    @Override
    public void saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        String[] usersIdString = knowledgeBase.getAuthorizedCharacter().split(",");
        List<AiSysUser> users = new ArrayList<>();
        for(String userIdString: usersIdString){
            AiSysUser user = aiSysUserRepository.findById(Long.parseLong(userIdString)).orElse(null);
            if(null!=user) users.add(user);
        }
        knowledgeBase.setUserPermissions(users);
        knowledgeBaseRepository.save(knowledgeBase);
    }

    @Override
    public List<KnowledgeBase> showKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    @Override
    public Flux<List<KnowledgeBase>> knowledgeBaseFlux() {
        sink.tryEmitNext(showKnowledgeBases());
        return sink.asFlux();
    }

    @Override
    public void emitKnowledgeBaseUpdate() {
        Mono.fromCallable(this::showKnowledgeBases)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(sink::tryEmitNext);
    }

    @Override
    public FileLog getByFilePath(String path) {
        return fileLogRepo.findOneByFilePath(path).orElse(null);
    }

    @Override
    public FileLog getById(Long id) {
        return fileLogRepo.findById(id).orElse(null);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseByTag(String tag) {
        return knowledgeBaseRepository.findOneByTag(tag).orElse(null);
    }

    @Override
    public boolean updateKnowledgeBaseInfo(KnowledgeBase knowledgeBase) {
        var old=knowledgeBaseRepository.findById(knowledgeBase.getId()).orElse(null);
        if(old==null)return false;
        copyNonNullProps(knowledgeBase, old);
        var userIds = knowledgeBase.getAuthorizedCharacter().split(",");
        List<AiSysUser> users = new ArrayList<>();
        for(String userIdString: userIds){
            aiSysUserRepository.findById(Long.parseLong(userIdString)).ifPresent(users::add);
        }
        if(!users.isEmpty())knowledgeBase.setUserPermissions(users);

        knowledgeBaseRepository.saveAndFlush(knowledgeBase);
        return true;
    }

    private void copyNonNullProps(KnowledgeBase src, KnowledgeBase target) {
        if (src.getName() != null) target.setName(src.getName());
        if (src.getDescription() != null) target.setDescription(src.getDescription());
        if (src.getTag() != null) target.setTag(src.getTag());
        if (src.getAuthorizedCharacter() != null) target.setAuthorizedCharacter(src.getAuthorizedCharacter());
    }


}
