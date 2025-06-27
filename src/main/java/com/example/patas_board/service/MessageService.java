package com.example.patas_board.service;

import com.example.patas_board.controller.form.MessageForm;
import com.example.patas_board.controller.form.UserMessageForm;
import com.example.patas_board.repository.MessageRepository;
import com.example.patas_board.repository.UserMessageRepository;
import com.example.patas_board.repository.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MessageService {
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    UserMessageRepository userMessageRepository;

    /*
     *投稿の登録
     */
    public void addMessage(MessageForm reqMessage) {
        Message message = setMessageEntity(reqMessage);
        messageRepository.save(message);
    }

    //MessageForm型をMessage型に変換
    private Message setMessageEntity(MessageForm reqMessage) {
        Message message = new Message();
        message.setId(reqMessage.getId());
        message.setTitle(reqMessage.getTitle());
        message.setText(reqMessage.getText());
        message.setCategory(reqMessage.getCategory());
        message.setUserId(reqMessage.getUserId());
        message.setUpdatedDate(new Date());
        return message;
    }

    /*
     * Messageを全件取得処理
     */
    public List<UserMessageForm> findAllMessage() {
        List<Message> results = userMessageRepository.findAllByOrderByCreatedDateDesc();
        List<UserMessageForm> messages = setUserMessageForm(results);
        return messages;
    }

    /*
     * ユーザーごとの投稿を取得
     */
    public List<UserMessageForm> findAllUserMessage(int id) {
        List<Message> results = userMessageRepository.findAllByUserId(id);
        List<UserMessageForm> messages = setUserMessageForm(results);
        return messages;
    }

    /*
     * レコード日付範囲取得処理
     */
    public List<UserMessageForm> findByCreatedDateMessage(String start, String end, String categoryText) throws ParseException {

        if (start == null || start.isEmpty()) {
            start = "2022-01-01 00:00:00";
        } else {
            start += " 00:00:00";
        }
        if (end == null || end.isEmpty()) {
            Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
            end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentTimestamp);
        } else {
            end += " 23:59:59";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Timestamp startDate = Timestamp.valueOf(start);
        Timestamp endDate = Timestamp.valueOf(end);

        List<Message> results;
        if(categoryText == null){
            results = userMessageRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(startDate, endDate);
        }else {
            results = userMessageRepository.findByCategoryContainingAndCreatedDateBetweenOrderByCreatedDateDesc(categoryText, startDate, endDate);
        }
        List<UserMessageForm> messages = setUserMessageForm(results);
        return messages;
    }

    //Message型をUserMessage型に変換
    private List<UserMessageForm> setUserMessageForm(List<Message> results) {
        List<UserMessageForm> messages = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            UserMessageForm message = new UserMessageForm();
            Message result = results.get(i);
            if (result.getUser() == null) {
                continue;
            }
            message.setId(result.getId());
            message.setTitle(result.getTitle());
            message.setText(result.getText());
            message.setUserId(result.getUserId());
            message.setCategory(result.getCategory());
            message.setName(result.getUser().getName());
            message.setAccount(result.getUser().getAccount());
            message.setBranchId(result.getUser().getBranchId());
            message.setDepartmentId(result.getUser().getDepartmentId());
            message.setCreatedDate(result.getCreatedDate());

            messages.add(message);
        }
        return messages;
    }

    //Page型のコンテンツをUserMessage型に変換
    public List<UserMessageForm> setUserMessageForm(Page<Message> results) {
        List<UserMessageForm> messages = new ArrayList<>();

        int q = results.getContent().size();

        //Listのひとつずつ取り出し
        for (int i = 0; i < results.getContent().size(); i++) {
            UserMessageForm message = new UserMessageForm();
            Message result = results.getContent().get(i);
            //MessageのuserIdのユーザーが存在しない時スキップ
            if (result.getUser() == null) {
                continue;
            }
            message.setId(result.getId());
            message.setTitle(result.getTitle());
            message.setText(result.getText());
            message.setUserId(result.getUserId());
            message.setCategory(result.getCategory());
            message.setName(result.getUser().getName());
            message.setAccount(result.getUser().getAccount());
            message.setBranchId(result.getUser().getBranchId());
            message.setDepartmentId(result.getUser().getDepartmentId());
            message.setCreatedDate(result.getCreatedDate());

            // 時間差
            Date oldTime = result.getCreatedDate();
            Date currentTime = new Date();
            long diffTime = currentTime.getTime() - oldTime.getTime();
            String diffTimeDate = String.valueOf(diffTime / (1000 * 60 * 60 * 24));

            message.setDiffTime(diffTimeDate + "日前");

            messages.add(message);
        }
        return messages;
    }

    //投稿の削除
    public void delete(Integer id) {
        messageRepository.deleteById(id);
    }

    // Page型のMessageを返す
    public Page<Message> findPages(Pageable pageable) {
        return messageRepository.findAllByOrderByCreatedDateDesc(pageable);
    }

    //ページ表示を表すリストを作成するメソッド
    public  List<Integer> pageList(int currentPage, int lastPage){
        List<Integer> pageList = new ArrayList<>();

        //総ページ数が7より小さいときは全ページを表示する
        if(lastPage < 7){
            for(int i=0;i<lastPage;i++){
                pageList.add(i+1);
            }
            return pageList;
        }
        //最初の5ページまでの間
        if(currentPage < 5){
            pageList.addAll(Arrays.asList(1,2,3,4,5,0,lastPage));
        }
        //最後の5ページのとき
        else if (currentPage > lastPage-4){
            pageList.addAll(Arrays.asList(1,0,lastPage-4,lastPage-3,lastPage-2,lastPage-1,lastPage));
        }
        //途中のページの時
        else {
            pageList.addAll(Arrays.asList(1,0,currentPage-1,currentPage,currentPage+1,0,lastPage));
        }
        return pageList;
    }

    public List<UserMessageForm> getPageData (List<UserMessageForm> messageAllData, Pageable pegeable){
        List<UserMessageForm> pageData = messageAllData.stream()
                .skip(pegeable.getPageNumber()*pegeable.getPageSize())
                .limit(pegeable.getPageSize()).toList();

        return pageData;
    }
}
