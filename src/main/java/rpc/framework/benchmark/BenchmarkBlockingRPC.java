package rpc.framework.benchmark;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import rpc.framework.NettyRpc;
import rpc.framework.ProtoParamRpcServer;
import rpc.framework.RemoteException;
import rpc.framework.test.BenchmarkProto.benchmarkRequest;
import rpc.framework.test.BenchmarkProto.benchmarkResponse;

public class BenchmarkBlockingRPC {

  public static class ClientWrapper extends Thread {
    @SuppressWarnings("unused")
    public void run() {
      InetSocketAddress addr = new InetSocketAddress("localhost", 15001);
      BenchmarkInterface proxy = null;
      proxy =
          (BenchmarkInterface) NettyRpc.getProtoParamBlockingRpcProxy(
              BenchmarkInterface.class, addr);

      long start = System.currentTimeMillis();
      benchmarkRequest req = benchmarkRequest.newBuilder().setIn1(start).build();
      for (int i = 0; i < 10000; i++) {
        try {
          benchmarkResponse response = proxy.shoot(req);
        } catch (RemoteException e1) {
          System.out.println(e1.getMessage());
        }
      }
      long end = System.currentTimeMillis();
      System.out.println("elapsed time: " + (end - start) + "msc");

    }
  }

  public static interface BenchmarkInterface {
    public benchmarkResponse shoot(benchmarkRequest l) throws RemoteException;
  }

  public static class BenchmarkImpl implements BenchmarkInterface {
    @Override
    public benchmarkResponse shoot(benchmarkRequest l) {
      benchmarkResponse ret = benchmarkResponse.newBuilder().setRst(System.currentTimeMillis()).build();
      return ret;
    }

  }

  public static void main(String[] args) throws InterruptedException,
      RemoteException {

    ProtoParamRpcServer server =
        NettyRpc.getProtoParamRpcServer(new BenchmarkImpl(),
            new InetSocketAddress("localhost", 15001));

    server.start();
    Thread.sleep(1000);

    int numThreads = 10;
    ClientWrapper client[] = new ClientWrapper[numThreads];
    for (int i = 0; i < numThreads; i++) {
      client[i] = new ClientWrapper();
    }

    for (int i = 0; i < numThreads; i++) {
      client[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      client[i].join();
    }

    server.shutdown();
    System.exit(0);
  }
}
