package com.kobe.kkong.img.slider;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class ImageSlideshowWithFadeAndResize extends JFrame {
	private JLabel imageLabel;
	private JButton startButton, stopButton, addImagesButton;
	private JSpinner delaySpinner;
	private JSlider scaleSlider;
	private Timer timer;
	private ArrayList<ImageIcon> images = new ArrayList<>();
	private int currentIndex = 0;
	private float alpha = 1f;
	private Timer fadeTimer;
	private float fadeStep = 0.05f;
	private int fadeDelay = 30;
	private int imageScalePercent = 100;
	private BufferedImage currentBufferedImage = null;

	public ImageSlideshowWithFadeAndResize() {
		setTitle("Java 이미지 슬라이드쇼 - 페이드 + 크기조절 + 기능 추가");
		setSize(900, 650);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		imageLabel = new JLabel("", JLabel.CENTER) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (currentBufferedImage != null) {
					Graphics2D g2d = (Graphics2D) g.create();
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
					int x = (getWidth() - currentBufferedImage.getWidth()) / 2;
					int y = (getHeight() - currentBufferedImage.getHeight()) / 2;
					g2d.drawImage(currentBufferedImage, x, y, null);
					g2d.dispose();
				}
			}
		};
		imageLabel.setOpaque(true);
		imageLabel.setBackground(Color.BLACK);
		imageLabel.setTransferHandler(new TransferHandler() {
			public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}

			public boolean importData(TransferSupport support) {
				try {
					java.util.List<File> files = (java.util.List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					for (File file : files) {
						if (file.getName().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
							try {
								BufferedImage img = javax.imageio.ImageIO.read(file);
								if (img != null) {
									images.add(new ImageIcon(img));
								} else {
									System.out.println("[ERROR] 이미지 로딩 실패 (null): " + file.getName());
								}
							} catch (Exception ex) {
								System.out.println("[ERROR] 이미지 로딩 중 예외 발생: " + ex.getMessage());
							}
						}
					}
					if (!images.isEmpty()) {
						currentIndex = images.size() - 1;
						alpha = 1f;
						updateBufferedImage();
						repaint();
					}
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		});
		add(imageLabel, BorderLayout.CENTER);

		JPanel controlPanel = new JPanel();
		addImagesButton = new JButton("이미지 추가");
		startButton = new JButton("시작");
		stopButton = new JButton("정지");
		delaySpinner = new JSpinner(new SpinnerNumberModel(2000, 500, 10000, 500));

		scaleSlider = new JSlider(50, 200, 100);
		scaleSlider.setMajorTickSpacing(50);
		scaleSlider.setMinorTickSpacing(10);
		scaleSlider.setPaintTicks(true);
		scaleSlider.setPaintLabels(true);

		scaleSlider.addChangeListener(e -> {
			imageScalePercent = scaleSlider.getValue();
			updateBufferedImage();
			repaint();
		});

		controlPanel.add(new JLabel("전환 시간(ms):"));
		controlPanel.add(delaySpinner);
		controlPanel.add(new JLabel("이미지 크기:"));
		controlPanel.add(scaleSlider);
		controlPanel.add(addImagesButton);
		controlPanel.add(startButton);
		controlPanel.add(stopButton);
		add(controlPanel, BorderLayout.SOUTH);

		timer = new Timer(2000, e -> startFade());

		addImagesButton.addActionListener(e -> openFileChooser());
		startButton.addActionListener(e -> {
			if (!images.isEmpty()) {
				timer.setDelay((int) delaySpinner.getValue());
				timer.start();
			}
		});
		stopButton.addActionListener(e -> timer.stop());

		setVisible(true);
	}

	private void openFileChooser() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));
		fileChooser.setAccessory(new ImagePreviewAccessory(fileChooser));
		fileChooser.setCurrentDirectory(new File(getLastDirectory()));

		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles = fileChooser.getSelectedFiles();
			for (File file : selectedFiles) {
				images.add(new ImageIcon(file.getAbsolutePath()));
			}

			saveLastDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());

			if (!images.isEmpty()) {
				currentIndex = images.size() - 1;
				alpha = 1f;
				updateBufferedImage();
				repaint();
			}
		}
	}

	private void startFade() {
		alpha = 0f;
		if (fadeTimer != null && fadeTimer.isRunning()) {
			fadeTimer.stop();
		}
		currentIndex = (currentIndex + 1) % images.size();
		updateBufferedImage();

		fadeTimer = new Timer(fadeDelay, e -> {
			alpha += fadeStep;
			if (alpha >= 1f) {
				alpha = 1f;
				fadeTimer.stop();
			}
			repaint();
		});
		fadeTimer.start();
	}

	private void updateBufferedImage() {
		if (images.isEmpty()) return;
		ImageIcon icon = images.get(currentIndex);
		int scale = imageScalePercent;
		int width = icon.getIconWidth() * scale / 100;
		int height = icon.getIconHeight() * scale / 100;
		Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
		currentBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = currentBufferedImage.createGraphics();
		g2d.drawImage(scaled, 0, 0, null);
		g2d.dispose();
	}

	private String getLastDirectory() {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		return prefs.get("lastDir", System.getProperty("user.home"));
	}

	private void saveLastDirectory(String path) {
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		prefs.put("lastDir", path);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ImageSlideshowWithFadeAndResize app = new ImageSlideshowWithFadeAndResize();
			//app.runTest(); // ✅ 테스트 실행
		});
	}

	// 테스트용 이미지 자동 로딩
	public void runTest() {
		System.out.println("[TEST] 이미지 자동 테스트 시작");

		// 테스트용 이미지 경로 (사용자 시스템에 맞게 조정 필요)
		String testImagePath = "/Users/kobe/Desktop/sample.jpg"; // macOS/Linux
		// String testImagePath = System.getProperty("user.home") + "\\Pictures\\test.jpg"; // Windows

		File testFile = new File(testImagePath);
		if (!testFile.exists()) {
			System.out.println("[ERROR] 테스트 이미지가 존재하지 않습니다: " + testFile.getAbsolutePath());
			return;
		}

		ImageIcon testIcon = new ImageIcon(testFile.getAbsolutePath());
		if (testIcon.getIconWidth() <= 0 || testIcon.getIconHeight() <= 0) {
			System.out.println("[ERROR] 이미지 로딩 실패: 잘못된 파일입니다.");
			return;
		}

		images.clear();
		images.add(testIcon);
		currentIndex = 0;
		alpha = 1f;
		updateBufferedImage();
		repaint();

		System.out.println("[PASS] 테스트 이미지 로딩 및 표시 성공");
	}
}

class ImagePreviewAccessory extends JPanel implements PropertyChangeListener {
	private JLabel previewLabel;
	private JFileChooser chooser;

	public ImagePreviewAccessory(JFileChooser chooser) {
		this.chooser = chooser;
		chooser.addPropertyChangeListener(this);

		setPreferredSize(new Dimension(200, 200));
		previewLabel = new JLabel();
		previewLabel.setHorizontalAlignment(JLabel.CENTER);
		setLayout(new BorderLayout());
		add(previewLabel, BorderLayout.CENTER);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
			File file = (File) evt.getNewValue();
			if (file != null && file.isFile()) {
				String name = file.getName().toLowerCase();
				if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif")) {
					ImageIcon icon = new ImageIcon(file.getAbsolutePath());
					Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
					previewLabel.setIcon(new ImageIcon(img));
				} else {
					previewLabel.setIcon(null);
				}
			} else {
				previewLabel.setIcon(null);
			}
		}
	}
}
