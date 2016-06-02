import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

/**
 * Process in Chandi Lamport algorithm
 * 
 * @author Akshai Prabhu
 *
 */
class CLProcess {
	int balance; // process balance
	int c1; // channel value between current and first process
	int c2; // channel value between current and second process
	int count1, count2; // indicates if marker is sent or not

	/**
	 * Constructor
	 */
	CLProcess() {
		balance = 1000;
		c1 = 0;
		c2 = 0;
		count1 = 0;
		count2 = 0;
	}

	/**
	 * To update the balance in each transaction
	 * 
	 * @param money
	 * @param event
	 */
	public void changeBalance(int money, int event) {
		synchronized (this) { // synchronized block
			if (event == 0) { // send money to another process
				System.out.println("Send amount: $" + money);
				System.out.println("Before sending: $" + balance);
				this.balance -= money;
				sendMoney(money);
				System.out.println("After sending: $" + balance);
				System.out.println("===========================" + "=====================================");
			} else if (event == 10) { // Receive money from different process
				System.out.println("Received amount: $" + money);
				System.out.println("Before receiving deposit: $" + balance);
				this.balance += money;
				if (count1 != 0) { // update channel value if marker not
									// returned
					c1 += money;
				}
				System.out.println("After receiving deposit: $" + balance);
				System.out.println("===========================" + "=====================================");
			} else if (event == 11) {
				System.out.println("Received amount: $" + money);
				System.out.println("Before receiving deposit: $" + balance);
				this.balance += money;
				if (count2 != 0) { // update channel value if marker not
									// returned
					c2 += money;
				}
				System.out.println("After receiving deposit: $" + balance);
				System.out.println("===========================" + "=====================================");
			} else if (event == 2) {
				this.balance += 0;
			} else if (event == 3) { // update count to indicate me
				++count1;
			} else if (event == 5) {
				++count2;
			} else {
				count1 = 0;
				count2 = 0;
				c1 = 0;
				c2 = 0;
			}
		}

	}

	/**
	 * To send money to a different process
	 * 
	 * @param money
	 */
	private void sendMoney(int money) {
		Random random = new Random();
		int server = random.nextInt(2);
		try {
			if (InetAddress.getLocalHost().getHostName().equals("glados")) {
				if (server == 0) {
					send("129.21.37.18", 40000, money);
				} else {
					send("129.21.37.16", 50000, money);
				}
			} else if (InetAddress.getLocalHost().getHostName().equals("kansas")) {
				if (server == 0) {
					send("129.21.37.16", 40000, money);
				} else {
					send("129.21.22.196", 50000, money);
				}
			} else if (InetAddress.getLocalHost().getHostName().equals("newyork")) {
				if (server == 0) {
					send("129.21.22.196", 40000, money);
				} else {
					send("129.21.37.18", 50000, money);
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sends money using TCP client
	 * 
	 * @param IP
	 * @param port
	 * @param money
	 */
	private void send(String IP, int port, int money) {
		Socket socket;
		try {
			socket = new Socket(IP, port);
			OutputStream outToServer = socket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF("" + money);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

/**
 * An event in the process
 * 
 * @author Akshai Prabhu
 *
 */
class CLEvent extends Thread {
	CLProcess p;
	int flag;

	/**
	 * Contructor
	 * 
	 * @param p
	 */
	public CLEvent(CLProcess p) {
		this.p = p;
		flag = 0;
	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Random random = new Random();
			int money = random.nextInt(100);
			p.changeBalance(money, 0);

			try {
				if (InetAddress.getLocalHost().getHostName().equals("glados")) {

					if (flag < 2) {
						++flag;
					}
					if (flag == 2) {
						flag = 0;// reset values before new snapshot
						p.count1 = 0;
						p.count2 = 0;
						p.c1 = 0;
						p.c2 = 0;
						System.out.println("Snapshot P1: $" + p.balance);
						sendMarker("129.21.37.18"); // send marker to other
													// processes
						sendMarker("129.21.37.16");
						p.changeBalance(0, 3); // start listening to channel
						p.changeBalance(0, 5);
					}
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * To send marker to other processes
	 * 
	 * @param IP
	 */
	private void sendMarker(String IP) {
		Socket socket;
		try {
			socket = new Socket(IP, 30000);
			OutputStream outToServer = socket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			// send marker 'M'
			out.writeUTF("M");
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

/**
 * To receive money from first process
 * 
 * @author Akshai Prabhu
 *
 */
class CLReceiveMoneyOne extends Thread {
	CLProcess p;
	ServerSocket serverSocket;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public CLReceiveMoneyOne(CLProcess p) {
		this.p = p;
	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(40000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				socket.close();
				serverSocket.close();
				p.changeBalance(Integer.parseInt(message), 10); // update with
																// received
																// money

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * To receive transaction from second process
 * 
 * @author Akshai Prabhu
 *
 */
class CLReceiveMoneyTwo extends Thread {
	CLProcess p;
	ServerSocket serverSocket;

	public CLReceiveMoneyTwo(CLProcess p) {
		this.p = p;

	}

	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(50000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				socket.close();
				serverSocket.close();
				p.changeBalance(Integer.parseInt(message), 11); // update with
																// received
																// money
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * Listens to marker from Process 1
 * 
 * @author Akshai Prabhu
 *
 */
class ReceiveMarker extends Thread {
	CLProcess p;
	ServerSocket serverSocket;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public ReceiveMarker(CLProcess p) {
		this.p = p;
	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(30000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				p.changeBalance(0, 2);
				sendSnapshot(p.balance);
				socket.close();
				serverSocket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send snapshot to process 1
	 * 
	 * @param balance
	 */
	private void sendSnapshot(int balance) {
		Socket socket;
		try {
			// check which process this is
			if (InetAddress.getLocalHost().getHostName().equals("kansas")) {
				socket = new Socket("129.21.22.196", 35000);
			} else {
				socket = new Socket("129.21.22.196", 45000);
			}
			OutputStream outToServer = socket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			p.changeBalance(0, 2);
			// check which process this is
			if (InetAddress.getLocalHost().getHostName().equals("kansas")) {
				out.writeUTF("" + p.balance + "\nChannel P1 - P2: 0\nChannel P3 - P2: 0");
			} else {
				out.writeUTF("" + p.balance + "\nChannel P1 - P3: 0\nChannel P2 - P3: 0");
			}
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * Receive snapshot from first process
 * 
 * @author Akshai Prabhu
 *
 */
class ReceiveSnapshotOne extends Thread {
	CLProcess p;
	ServerSocket serverSocket;

	/**
	 * Contructor
	 * 
	 * @param p
	 */
	public ReceiveSnapshotOne(CLProcess p) {
		this.p = p;

	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(35000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF(); // snapshot of process 2
				socket.close();
				serverSocket.close();

				System.out.println("Snapshot P2: " + message);
				System.out.println("Channel P2 - P1: " + p.c1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * Receive snapshot from second process
 * 
 * @author Akshai Prabhu
 *
 */
class ReceiveSnapshotTwo extends Thread {
	CLProcess p;
	ServerSocket serverSocket;

	/**
	 * Constructor
	 * 
	 * @param p
	 */
	public ReceiveSnapshotTwo(CLProcess p) {
		this.p = p;

	}

	/**
	 * Thread run method
	 */
	public void run() {
		while (true) {
			Socket socket;
			String message = new String();
			try {
				serverSocket = new ServerSocket(45000);
				socket = serverSocket.accept();
				DataInputStream in = new DataInputStream(socket.getInputStream());
				message = in.readUTF();
				socket.close();
				serverSocket.close();
				p.changeBalance(0, 5);

				System.out.println("Snapshot P3: " + message);
				System.out.println("Channel P3 - P1: " + p.c2);
				System.out.println("===========================" + "=====================================");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/**
 * Main class that initiates all the other thread
 * 
 * @author Akshai Prabhu
 *
 */
public class ChandiLamport {
	public static void main(String args[]) {
		CLProcess p = new CLProcess();
		CLEvent e = new CLEvent(p);
		CLReceiveMoneyOne rmo = new CLReceiveMoneyOne(p);
		CLReceiveMoneyTwo rmt = new CLReceiveMoneyTwo(p);
		ReceiveMarker rm = new ReceiveMarker(p);
		ReceiveSnapshotOne rso = new ReceiveSnapshotOne(p);
		ReceiveSnapshotTwo rst = new ReceiveSnapshotTwo(p);
		rmo.start();
		rmt.start();
		rm.start();
		rso.start();
		rst.start();
		Scanner sc = new Scanner(System.in);
		System.out.println("Press Enter to Start");
		String in = sc.nextLine();
		sc.close();
		e.start();
	}
}
