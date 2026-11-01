#include "core/database/Database.h"

#include <drogon/drogon.h>

namespace core::database
{
drogon::orm::DbClientPtr client()
{
    return drogon::app().getDbClient("default");
}
}
