package priv.xds.listener;

import com.mysql.cj.protocol.MessageSender;
import love.forte.simbot.annotation.Filter;
import love.forte.simbot.annotation.OnGroup;
import love.forte.simbot.api.message.events.GroupMsg;
import love.forte.simbot.api.message.results.GroupMemberInfo;
import love.forte.simbot.api.sender.MsgSender;
import love.forte.simbot.filter.MatchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import priv.xds.annotation.RoleCheck;
import priv.xds.exception.UnNecessaryInvokeException;
import priv.xds.pojo.User;
import priv.xds.service.UserService;
import priv.xds.util.MessageUtil;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author "DeSen Xu"
 * @date 2021-09-14 12:14
 */
@Component
public class CommandListener {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @OnGroup
    @Filter(value = "提醒未打卡", matchType = MatchType.EQUALS)
    public void getUnsigned(GroupMsg groupMsg, MsgSender sender) {
        boolean admin = groupMsg.getPermission().isAdmin() || groupMsg.getPermission().isOwner();
        if (admin) {
            List<User> unsignedUsers = userService.getUnsignedUsers(groupMsg.getGroupInfo().getGroupCode());
            if (unsignedUsers == null) {
                sender.SENDER.sendGroupMsg(groupMsg, "真好,今天所有人都打卡了");
            } else {
                StringBuilder builder = new StringBuilder(20);
                for (User unsignedUser : unsignedUsers) {
                    builder.append(MessageUtil.atSomeone(unsignedUser.getQq()));
                    builder.append(" ");
                }
                builder.append(" 记得打卡!");
                sender.SENDER.sendGroupMsg(groupMsg, builder.toString());
            }
        } else {
            sender.SENDER.sendGroupMsg(groupMsg, MessageUtil.atSomeone(groupMsg) + "宝,你的权限不足!");
        }
    }

    @OnGroup
    @Filter(value = "^忽略 \\d+", matchType = MatchType.REGEX_FIND, trim = true)
    @RoleCheck(role = 2)
    public void ignoreSomeone(GroupMsg groupMsg, MsgSender sender) {
        String text = groupMsg.getText();
        assert text != null;
        String[] s = text.split(" ");
        String qq = text.split(" ")[1];
        String groupCode = groupMsg.getGroupInfo().getGroupCode();
        String accountNickname = null;
        try {
            userService.ignoreUser(qq, groupCode);
            accountNickname = sender.GETTER.getMemberInfo(groupCode, qq).getAccountNickname();
            sender.SENDER.sendGroupMsg(groupMsg, "已经忽略对" + qq + "(" + accountNickname +")的打卡统计");
        } catch (UnNecessaryInvokeException e) {
            sender.SENDER.sendGroupMsg(groupMsg, qq + "(" + accountNickname +")已经被忽略了");
        } catch (NoSuchElementException e) {
            sender.SENDER.sendGroupMsg(groupMsg, MessageUtil.atSomeone(groupMsg) + "无法找到目标用户!请检查后重新输入");
        }
    }

    @OnGroup
    @Filter(value = "^取消忽略 \\d+", matchType = MatchType.REGEX_FIND, trim = true)
    @RoleCheck(role = 2)
    public void reStatisticsUser(GroupMsg groupMsg, MsgSender sender) {
        String text = groupMsg.getText();
        assert text != null;
        String[] s = text.split(" ");
        String qq = text.split(" ")[1];
        String groupCode = groupMsg.getGroupInfo().getGroupCode();
        String accountNickname = null;
        try {
            userService.reStatisticsUser(qq, groupCode);
            accountNickname = sender.GETTER.getMemberInfo(groupCode, qq).getAccountNickname();
            sender.SENDER.sendGroupMsg(groupMsg, "已经重新开始统计" + qq + "(" + accountNickname +")的打卡统计");
        } catch (UnNecessaryInvokeException e) {
            sender.SENDER.sendGroupMsg(groupMsg, MessageUtil.atSomeone(groupMsg) + "无法找到目标用户!请检查后重新输入");
        } catch (NoSuchElementException e) {
            sender.SENDER.sendGroupMsg(groupMsg, qq + "(" + accountNickname +")仍然还在统计");
        }
    }

    @OnGroup
    @Filter(value = "打卡情况", matchType = MatchType.EQUALS, trim = true)
    public void getUnsignedUser(GroupMsg groupMsg, MsgSender sender) {
        List<User> unsignedUsers = userService.getUnsignedUsers(groupMsg.getGroupInfo().getGroupCode());
        if (unsignedUsers == null) {
            sender.SENDER.sendGroupMsg(groupMsg, "真好,今天所有人都打卡了");
            return;
        }
        StringBuilder builder = new StringBuilder(20);
        builder.append("目前还没打卡的人: ");
        for (User unsignedUser : unsignedUsers) {
            GroupMemberInfo memberInfo = sender.GETTER.getMemberInfo(groupMsg.getGroupInfo().getGroupCode(), unsignedUser.getQq());
            builder
                    .append(memberInfo.getAccountCode())
                    .append("(")
                    .append(memberInfo.getAccountNickname())
                    .append(") ");
        }
        sender.SENDER.sendGroupMsg(groupMsg, builder.toString());
    }
}