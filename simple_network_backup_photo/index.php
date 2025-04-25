<?php
header('Content-Type: application/json');

// 允许跨域请求（根据需要设置）
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// 上传目录设置
$uploadDir = 'uploads/';
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

$response = [
    'success' => false,
    'message' => '',
    'filePath' => ''
];

try {
    // 检查是否有文件上传
    if (!isset($_FILES['file'])) {
        throw new Exception('没有接收到文件');
    }

    $file = $_FILES['file'];
    
    // 检查上传错误
    if ($file['error'] !== UPLOAD_ERR_OK) {
        throw new Exception('文件上传错误: ' . $file['error']);
    }
    
    // 验证文件类型（安全考虑）
    $allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];
    $fileInfo = finfo_open(FILEINFO_MIME_TYPE);
    $detectedType = finfo_file($fileInfo, $file['tmp_name']);
    finfo_close($fileInfo);
    
    if (!in_array($detectedType, $allowedTypes)) {
        throw new Exception('不允许的文件类型: ' . $detectedType);
    }
    
    // 获取原始文件名（从Content-Disposition中获取）
    // 获取原始文件名
    $filename = $file['name'];
    
    // 获取显示名称（如果客户端提供了）
    $filename = $_POST['filename'] ?? $filename;

    // 生成安全的文件名
    $safeFilename = preg_replace("/[^a-zA-Z0-9._-]/", "", $filename);
    $targetPath = $uploadDir . $safeFilename;
    
    // 移动文件到目标位置
    if (move_uploaded_file($file['tmp_name'], $targetPath)) {
        $response['success'] = true;
        $response['message'] = '文件上传成功';
        $response['filePath'] = $targetPath;
    } else {
        throw new Exception('无法保存文件');
    }
} catch (Exception $e) {
    $response['message'] = $e->getMessage();
}

echo json_encode($response);
?>