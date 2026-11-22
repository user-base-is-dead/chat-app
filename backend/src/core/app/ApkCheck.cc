#include "core/app/ApkCheck.h"

#include <cstdint>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <mutex>
#include <unordered_map>
#include <vector>

#include <zlib.h>

namespace core::app
{
namespace
{
constexpr const char *kPackageName = "com.civenth.app";
constexpr const char *kDownloadPath = "/v1/app/download/";
constexpr const char *kReleaseDir = "releases";
constexpr size_t kMaxManifest = 4 * 1024 * 1024;

uint16_t u16(const std::string &data, size_t at)
{
    if (at + 2 > data.size())
        return 0;
    return static_cast<uint16_t>(static_cast<uint8_t>(data[at]) | static_cast<uint8_t>(data[at + 1]) << 8);
}

uint32_t u32(const std::string &data, size_t at)
{
    if (at + 4 > data.size())
        return 0;
    return static_cast<uint32_t>(u16(data, at)) | static_cast<uint32_t>(u16(data, at + 2)) << 16;
}

std::optional<std::string> inflateRaw(const std::string &input, size_t expected)
{
    if (expected == 0 || expected > kMaxManifest)
        return std::nullopt;
    std::string output(expected, '\0');
    z_stream stream{};
    if (inflateInit2(&stream, -MAX_WBITS) != Z_OK)
        return std::nullopt;
    stream.next_in = reinterpret_cast<Bytef *>(const_cast<char *>(input.data()));
    stream.avail_in = static_cast<uInt>(input.size());
    stream.next_out = reinterpret_cast<Bytef *>(output.data());
    stream.avail_out = static_cast<uInt>(output.size());
    const auto status = inflate(&stream, Z_FINISH);
    const auto written = stream.total_out;
    inflateEnd(&stream);
    if (status != Z_STREAM_END || written != expected)
        return std::nullopt;
    return output;
}

std::optional<std::string> manifestFrom(const std::string &zip)
{
    if (zip.size() < 22)
        return std::nullopt;
    size_t end = std::string::npos;
    const size_t floor = zip.size() > 65557 ? zip.size() - 65557 : 0;
    for (size_t at = zip.size() - 22 + 1; at-- > floor;)
        if (u32(zip, at) == 0x06054b50)
        {
            end = at;
            break;
        }
    if (end == std::string::npos)
        return std::nullopt;

    const auto entries = u16(zip, end + 10);
    size_t at = u32(zip, end + 16);
    for (uint16_t i = 0; i < entries && u32(zip, at) == 0x02014b50; ++i)
    {
        const auto method = u16(zip, at + 10);
        const auto packed = u32(zip, at + 20);
        const auto plain = u32(zip, at + 24);
        const auto nameLength = u16(zip, at + 28);
        const auto extraLength = u16(zip, at + 30);
        const auto commentLength = u16(zip, at + 32);
        const auto local = u32(zip, at + 42);
        const auto name = zip.substr(at + 46, nameLength);
        at += 46 + nameLength + extraLength + commentLength;
        if (name != "AndroidManifest.xml")
            continue;
        if (u32(zip, local) != 0x04034b50)
            return std::nullopt;
        const size_t data = local + 30 + u16(zip, local + 26) + u16(zip, local + 28);
        if (data + packed > zip.size())
            return std::nullopt;
        const auto body = zip.substr(data, packed);
        if (method == 0)
            return plain == packed ? std::optional<std::string>(body) : std::nullopt;
        if (method == 8)
            return inflateRaw(body, plain);
        return std::nullopt;
    }
    return std::nullopt;
}

std::vector<std::string> readPool(const std::string &xml, size_t chunk)
{
    std::vector<std::string> strings;
    const auto headerSize = u16(xml, chunk + 2);
    const auto count = u32(xml, chunk + 8);
    const bool utf8 = (u32(xml, chunk + 16) & 0x100) != 0;
    const size_t start = chunk + u32(xml, chunk + 20);
    if (count > 100000)
        return strings;
    for (uint32_t i = 0; i < count; ++i)
    {
        size_t at = start + u32(xml, chunk + headerSize + i * 4);
        std::string value;
        if (utf8)
        {
            if (at < xml.size() && (static_cast<uint8_t>(xml[at]) & 0x80))
                at += 2;
            else
                at += 1;
            size_t length = at < xml.size() ? static_cast<uint8_t>(xml[at]) : 0;
            if (at < xml.size() && (length & 0x80))
            {
                length = ((length & 0x7f) << 8) | (at + 1 < xml.size() ? static_cast<uint8_t>(xml[at + 1]) : 0);
                at += 2;
            }
            else
                at += 1;
            if (at + length <= xml.size())
                value = xml.substr(at, length);
        }
        else
        {
            size_t length = u16(xml, at);
            at += 2;
            if (length & 0x8000)
            {
                length = ((length & 0x7fff) << 16) | u16(xml, at);
                at += 2;
            }
            for (size_t c = 0; c < length && at + c * 2 + 1 < xml.size(); ++c)
            {
                const auto ch = u16(xml, at + c * 2);
                value += ch < 0x80 ? static_cast<char>(ch) : '?';
            }
        }
        strings.push_back(std::move(value));
    }
    return strings;
}

std::optional<ApkInfo> parseManifest(const std::string &xml)
{
    if (u16(xml, 0) != 0x0003)
        return std::nullopt;
    std::vector<std::string> pool;
    size_t at = u16(xml, 2);
    while (at + 8 <= xml.size())
    {
        const auto type = u16(xml, at);
        const auto size = u32(xml, at + 4);
        if (size < 8 || at + size > xml.size())
            return std::nullopt;
        if (type == 0x0001)
            pool = readPool(xml, at);
        else if (type == 0x0102)
        {
            const auto nameIndex = u32(xml, at + 20);
            if (nameIndex >= pool.size() || pool[nameIndex] != "manifest")
                return std::nullopt;
            const size_t attrs = at + 16 + u16(xml, at + 24);
            const auto attrSize = u16(xml, at + 26);
            const auto attrCount = u16(xml, at + 28);
            ApkInfo info;
            bool hasVersion = false;
            for (uint16_t i = 0; i < attrCount; ++i)
            {
                const size_t attr = attrs + i * attrSize;
                const auto name = u32(xml, attr + 4);
                if (name >= pool.size())
                    continue;
                const auto raw = u32(xml, attr + 8);
                const auto dataType = static_cast<uint8_t>(attr + 15 < xml.size() ? xml[attr + 15] : 0);
                const auto data = u32(xml, attr + 16);
                if (pool[name] == "package" && raw < pool.size())
                    info.packageName = pool[raw];
                else if (pool[name] == "versionCode" && (dataType == 0x10 || dataType == 0x11))
                {
                    info.versionCode = static_cast<int>(data);
                    hasVersion = true;
                }
            }
            if (info.packageName.empty() || !hasVersion)
                return std::nullopt;
            return info;
        }
        at += size;
    }
    return std::nullopt;
}

struct Cached
{
    uintmax_t size = 0;
    std::filesystem::file_time_type modified;
    std::optional<ApkInfo> info;
};

std::mutex cacheMutex;
std::unordered_map<std::string, Cached> cache;
}

std::optional<ApkInfo> readApk(const std::string &path)
{
    std::error_code error;
    const auto size = std::filesystem::file_size(path, error);
    if (error)
        return std::nullopt;
    const auto modified = std::filesystem::last_write_time(path, error);
    if (error)
        return std::nullopt;
    {
        std::lock_guard lock(cacheMutex);
        const auto it = cache.find(path);
        if (it != cache.end() && it->second.size == size && it->second.modified == modified)
            return it->second.info;
    }
    std::ifstream file(path, std::ios::binary);
    const std::string zip((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
    const auto manifest = manifestFrom(zip);
    const auto info = manifest ? parseManifest(*manifest) : std::nullopt;
    std::lock_guard lock(cacheMutex);
    cache[path] = Cached{size, modified, info};
    return info;
}

bool releaseFileMatches(const std::string &downloadUrl, int versionCode)
{
    const auto at = downloadUrl.find(kDownloadPath);
    if (at == std::string::npos)
        return true;
    const auto name = downloadUrl.substr(at + std::strlen(kDownloadPath));
    if (name.empty() || name.find('/') != std::string::npos || name.find("..") != std::string::npos)
        return false;
    const auto info = readApk((std::filesystem::path(kReleaseDir) / name).string());
    return info && info->packageName == kPackageName && info->versionCode == versionCode;
}
}
