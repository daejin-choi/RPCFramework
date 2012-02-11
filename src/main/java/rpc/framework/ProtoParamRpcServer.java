package rpc.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import rpc.framework.ProtoParamRpcProtos.Invocation;
import rpc.framework.ProtoParamRpcProtos.Response;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class ProtoParamRpcServer extends NettyServerBase {
  private static Log LOG = LogFactory.getLog(ProtoParamRpcServer.class);
  private final Object instance;
  private final Class<?> clazz;
  private final ChannelPipelineFactory pipeline;
  private Map<String, Method> methods;

  public ProtoParamRpcServer(Object proxy, InetSocketAddress bindAddress) {
    super(bindAddress);
    this.instance = proxy;
    this.clazz = instance.getClass();
    this.methods = new HashMap<String, Method>();
    this.pipeline =
        new ProtoPipelineFactory(new ServerHandler(),
            Invocation.getDefaultInstance());

    super.init(this.pipeline);

    for (Method m : this.clazz.getMethods()) {
      methods.put(m.getName(), m);
    }
  }

  private class ServerHandler extends SimpleChannelUpstreamHandler {
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
      final Invocation request = (Invocation) e.getMessage();
      Object res = null;
      Response response = null;

      try {
        String methodName = request.getMethodName();
        if (methods.containsKey(methodName) == false) {
          throw new NoSuchMethodException(methodName);
        }

        Method method = methods.get(methodName);

        Object obj = null;
        Class<?> params[] = method.getParameterTypes();
        Method mtd =
            params[0].getMethod("parseFrom", new Class[] { ByteString.class });
        obj = mtd.invoke(null, request.getParam(0));

        res = method.invoke(instance, obj);

      } catch (InvocationTargetException internalException) {
        response =
            Response
                .newBuilder()
                .setId(request.getId())
                .setHasReturn(false)
                .setExceptionMessage(
                    internalException.getTargetException().toString()).build();
        e.getChannel().write(response);
        return;
      } catch (Exception otherException) {
        response =
            Response.newBuilder().setId(request.getId()).setHasReturn(false)
                .setExceptionMessage(otherException.toString()).build();
        e.getChannel().write(response);
        return;
      }

      if (res == null) {
        response =
            Response.newBuilder().setId(request.getId()).setHasReturn(false)
                .build();
      } else {
        ByteString str = ((Message) res).toByteString();
        response =
            Response.newBuilder().setId(request.getId()).setHasReturn(true)
                .setReturnValue(str).build();
      }
      e.getChannel().write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
      e.getChannel().close();
      LOG.error(e.getCause());
    }
  }
}
