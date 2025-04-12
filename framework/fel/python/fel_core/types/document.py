# -- encoding: utf-8 --
# Copyright (c) Huawei Technologies Co., Ltd. -. All rights reserved.
import typing

from fel_core.types.serializable import Serializable
from fel_core.types.media import Media


class Document(Serializable):
    """
    Document.
    """
    content: str
    media: Media = None
    metadata: typing.Dict[str, object]

    class Config:
        frozen = True
        smart_union = True
