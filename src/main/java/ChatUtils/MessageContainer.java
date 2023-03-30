package ChatUtils;

import ChatActions.ChatActions;
import ChatGui.MainChatFrame;
import ChatSchema.ChatMessage;
import ChatSchema.GroupMessage;

import javax.swing.text.PlainDocument;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageContainer {

    private int selfId;
    private int toId;
    private PlainDocument document;
    private final Object documentLock = new Object();
    private final MainChatFrame mainChatFrame;

    private AtomicBoolean isDocSynced = new AtomicBoolean(false);

    private Queue<String> waitingQueue;

    private Queue<String> bufferQueue;

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getToId() {
        return toId;
    }

    public MessageContainer(int selfId, int toId, MainChatFrame mainChatFrame) {
        this.selfId = selfId;
        this.toId = toId;
        this.mainChatFrame = mainChatFrame;
        waitingQueue = new ConcurrentLinkedQueue<>();
        bufferQueue = new ConcurrentLinkedQueue<>();
        startSync();
    }

    public void startSync() {
        System.out.println(toId + " start record sync ...");
        isDocSynced.set(false);
        sync();
    }

    public void loadSyncResult(String message) {
        bufferQueue.add(message);
    }

    public void finishSync(TinyJson json) {
        if(isDocSynced.get()){
            return;
        }
        TinyJson[] data = (TinyJson[]) json.getPrimitive("data");
        if (toId == 0) {
            for (var j : data) {
                GroupMessage groupMessage = j.fillObject(new GroupMessage());
                bufferQueue.add(groupMessage.format(selfId == groupMessage.sendUserId));
            }
        } else {
            for (var j : data) {
                ChatMessage chatMessage = j.fillObject(new ChatMessage());
                bufferQueue.add(chatMessage.format(selfId == chatMessage.fromUserId));
            }
        }
        dump();
        isDocSynced.set(true);
        dumpWaiting();
        System.out.println(toId + " finish record sync!");
    }

    private boolean frameMatch() {
        return toId == mainChatFrame.getCurrentTalkingUser();
    }

    public void addMessage(String message) {
        if (frameMatch()) {
            if (!bufferQueue.isEmpty())
                dump();
            mainChatFrame.updateOnReceive(message);
        } else {
            bufferQueue.add(message);
        }
    }

    private void dumpWaiting() {
        while (waitingQueue.size() != 0) {
            bufferQueue.add(waitingQueue.poll());
        }
    }

    public void dump() {
        while (frameMatch() && bufferQueue.size() != 0) {
            mainChatFrame.updateOnReceive(bufferQueue.poll());
        }
    }

    private void sync() {
        boolean isGroup = toId == 0;
        var syncJson = ChatActions.SYNC_HISTORY.getJsonTemplate();
        syncJson.put("isGroup", isGroup);
        syncJson.put("fromUserId", selfId);
        syncJson.put("toUserId", toId);
        try{
            mainChatFrame.getGuiManager().getSocketClient().sendJson(syncJson);
        } catch (IOException e) {
            mainChatFrame.getGuiManager().raiseLethalError("服务器错误");
        }
    }

    public boolean isSynced() {
        return isDocSynced.get();
    }


}
