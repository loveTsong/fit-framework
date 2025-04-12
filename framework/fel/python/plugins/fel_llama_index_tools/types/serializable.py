# -- encoding: utf-8 --
# Copyright (c) Huawei Technologies Co., Ltd. -. All rights reserved.
import typing

try:
    import pydantic

    if pydantic.__version__.startswith("1."):
        raise ImportError
    import pydantic.v1 as pydantic
except ImportError:
    import pydantic


class Serializable(pydantic.BaseModel):
    def json(self, **kwargs: typing.Any) -> str:
        kwargs_with_defaults: typing.Any = {"by_alias": True, "exclude_unset": True, **kwargs}
        return super().json(**kwargs_with_defaults)

    def dict(self, **kwargs: typing.Any) -> typing.Dict[str, typing.Any]:
        kwargs_with_defaults: typing.Any = {"by_alias": True, "exclude_unset": True, **kwargs}
        return super().dict(**kwargs_with_defaults)