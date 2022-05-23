/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package counter;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import counter.rpc.CounterGrpcHelper;
import counter.rpc.Outter.ValueResponse;
import counter.rpc.Outter.UpdateRequest;
import counter.rpc.Outter.GetRequest;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

public class Client {
    static int operationTimeout = 5000;

    private static final CliClientServiceImpl cliClientService = new CliClientServiceImpl();

    private static PeerId leader;

    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage : provide args {groupId} {conf}");
            System.exit(1);
        }
        init(args[0], args[1]);

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                System.out.println("DLT Node exits.");
//            } catch (IOException e) { /* failed */ }
//        }));

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Input : ");
            String userInput = scanner.nextLine();
            System.out.println("Operation : " + userInput);

            final CountDownLatch latch = new CountDownLatch(1);

            String operation = userInput.split(" ")[0];

            switch (operation) {
                case "get":
                    get(leader, latch);
                    break;
                case "+":
                    String addValue = userInput.split(" ")[1];
                    incrementAndGet(leader, latch, Integer.parseInt(addValue));
                    break;
                case "-":
                    String minusValue = userInput.split(" ")[1];
                    decrementAndGet(leader, latch, -Integer.parseInt(minusValue));
                    break;
                case "exit":
                    System.exit(0);
                    break;
            }

            latch.await();
        }
    }

    private static void init(final String groupId, final String confStr) throws InterruptedException, TimeoutException {
        CounterGrpcHelper.initGRpc();
        final Configuration conf = new Configuration();
        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }
        RouteTable.getInstance().updateConfiguration(groupId, conf);
        cliClientService.init(new CliOptions());
        if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
            throw new IllegalStateException("Refresh leader failed");
        }

        leader = RouteTable.getInstance().selectLeader(groupId);
    }

    private static void update(
            final PeerId leader,
            CountDownLatch latch,
            final int change) throws RemotingException, InterruptedException
    {
        UpdateRequest request = UpdateRequest.newBuilder().setChange(change).build();

        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    ValueResponse response = (ValueResponse) result;
                    String status;

                    if (response.getSuccess())
                        status = "Success";
                    else
                        status = "Fail";

                    if (change >= 0)
                        System.out.println("Increment Counter : " + status);
                    else
                        System.out.println("Decrement Counter : " + status);

                    try {
                        get(leader, latch);
                    } catch (RemotingException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    err.printStackTrace();
                }
            }

            @Override
            public Executor executor() { return null; }
        };

        Client.cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void get(
            final PeerId leader,
            CountDownLatch latch) throws RemotingException, InterruptedException
    {
        GetRequest request = GetRequest.newBuilder().build();
        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    ValueResponse response = (ValueResponse) result;
                    System.out.println("Get Counter Value : " + response.getValue());
                } else {
                    err.printStackTrace();
                }

                latch.countDown();
            }

            @Override
            public Executor executor() { return null; }
        };

        Client.cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void incrementAndGet(
            final PeerId leader,
            CountDownLatch latch,
            final int change) throws RemotingException, InterruptedException
    {
        update(leader, latch, change);
    }

    private static void decrementAndGet(
            final PeerId leader,
            CountDownLatch latch,
            final int change) throws RemotingException, InterruptedException
    {
        update(leader, latch, change);
    }


}
