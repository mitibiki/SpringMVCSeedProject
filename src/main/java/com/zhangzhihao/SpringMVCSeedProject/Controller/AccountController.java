package com.zhangzhihao.SpringMVCSeedProject.Controller;

import com.zhangzhihao.SpringMVCSeedProject.Service.UserService;
import com.zhangzhihao.SpringMVCSeedProject.Utils.ValidateCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.zhangzhihao.SpringMVCSeedProject.Utils.LogUtils.LogToDB;
import static com.zhangzhihao.SpringMVCSeedProject.Utils.SHAUtils.getSHA_256;

@SuppressWarnings("JavaDoc")
@Controller
@RequestMapping("/Account")
public class AccountController {
    @Autowired
    UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 转向登录界面
     *
     * @return 登录界面
     */
    @RequestMapping(value = "/Login", method = RequestMethod.GET)
    public String LoginPage() {
        return "Account/Login";
    }

    /**
     * 接收用户登录传参，判断是否登陆成功
     *
     * @param UserName
     * @param Password
     * @param session
     * @param request
     * @return
     */
    @SuppressWarnings("ConstantConditions")
    @Cacheable("springCache")
    @RequestMapping(value = "/Login", method = RequestMethod.POST)
    public String Login(@RequestParam("UserName") String UserName,
                        @RequestParam("Password") String Password,
                        @RequestParam(value = "RememberMe",required = false) String RememberMe,
                        HttpSession session,
                        HttpServletRequest request) {
        String code = (String) session.getAttribute("validateCode");
        String submitCode = WebUtils.getCleanParam(request, "validateCode");
        if (StringUtils.isEmpty(submitCode) || !StringUtils.equals(code.toLowerCase(), submitCode.toLowerCase())) {
            return "redirect:/Account/Login";
        }
        UsernamePasswordToken token = null;
        try {
            Subject user = SecurityUtils.getSubject();
            token = new UsernamePasswordToken(UserName.trim(), getSHA_256(Password));
            if(RememberMe!=null&&RememberMe.equals("on"))
                token.setRememberMe(true);
            user.login(token);
            return "redirect:/";
        } catch (Exception e) {
            LogToDB(e);
            if (token != null) {
                token.clear();
            }
            return "redirect:/Account/Login";
        }
    }

    /**
     * 生成验证码
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/validateCode")
    public void validateCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-cache");
        String verifyCode = ValidateCode.generateTextCode(ValidateCode.TYPE_ALL_MIXED, 4, null);
        request.getSession().setAttribute("validateCode", verifyCode);
        response.setContentType("image/jpeg");
        BufferedImage bim = ValidateCode.generateImageCode(verifyCode, 90, 30, 3, true, Color.WHITE, Color.BLACK, null);
        ImageIO.write(bim, "JPEG", response.getOutputStream());
    }

    @RequestMapping("/LogOut")
    public void LogOut(){
        SecurityUtils.getSubject().logout();
    }
}
