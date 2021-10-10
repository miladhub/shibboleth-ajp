import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = "/info")
public class InfoServlet extends HttpServlet {
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        PrintWriter writer = response.getWriter();
        String uid = "<h2>uid = " + request.getAttribute("uid") + "</h2>";
        String eduAttribute = (String) request.getAttribute("eduPersonAffiliation");
        if (eduAttribute != null && !eduAttribute.isEmpty())
            eduAttribute = String.join(", ",
                    Arrays.asList(eduAttribute.split(";")));
        String eduPersonAffiliation = "<h2>eduPersonAffiliation = " + eduAttribute + "</h2>";
        String email = "<h2>email = " + request.getAttribute("email") + "</h2>";
        String html = "<html>" + uid + eduPersonAffiliation + email + "</html>";
        writer.println(html);
    }
}
